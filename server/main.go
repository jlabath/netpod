package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net"
	"os"
	"time"

	bencode "github.com/jackpal/bencode-go"
)

func greetHandler(encodedArgs []json.RawMessage) (json.RawMessage, error) {
	if len(encodedArgs) < 1 {
		return nil, fmt.Errorf("invalid number of arguments passed to server")
	}
	var name string

	if err := json.Unmarshal(encodedArgs[0], &name); err != nil {
		return nil, err
	}

	//sleep somewhat
	time.Sleep(time.Millisecond * time.Duration(rand.Intn(1000)+1))

	return json.Marshal(fmt.Sprintf("hi there %s", name))
}

func brokenHandler(encodedArgs []json.RawMessage) (json.RawMessage, error) {
	//sleep somewhat
	time.Sleep(time.Millisecond * time.Duration(rand.Intn(1000)+1))
	return nil, fmt.Errorf("this will fail")
}

//this is how one can talk to this server
//echo "Hello, World!" | nc -U ./server.sock

func main() {
	// Define the socket file path
	socketPath := "./server.sock"

	// Remove any existing socket file
	if err := os.Remove(socketPath); err != nil && !os.IsNotExist(err) {
		log.Fatalf("Failed to remove existing socket file: %v", err)
	}

	//init rand num gen
	rand.Seed(time.Now().UnixNano())

	//describe response
	describe := DescribeResponse{
		Format: "json",
		Namespaces: []Namespace{Namespace{
			Name: "sample.service",
			Vars: []Var{Var{
				Name:    "greet",
				Handler: greetHandler,
			},
				Var{
					Name:    "broken-func",
					Handler: brokenHandler,
				}},
		}},
	}
	//craft handelr map for fast lookups
	handlerMap := describe.handlerMap()

	// Create a UNIX domain socket listener
	l, err := net.Listen("unix", socketPath)
	if err != nil {
		log.Fatalf("Failed to listen on UNIX socket: %v", err)
	}
	defer l.Close()

	log.Printf("Listening on %s", socketPath)

	for {
		// Accept incoming connections
		conn, err := l.Accept()
		if err != nil {
			log.Printf("Failed to accept connection: %v", err)
			continue
		}
		log.Printf("Accepted connection %+v", conn)

		// Handle the connection
		go handleConnection(&describe, handlerMap, conn)
	}
}

func handleConnection(desc *DescribeResponse, handlerMap map[string]Handler, conn net.Conn) {
	defer conn.Close()
	var requestMsg Request
	if err := bencode.Unmarshal(conn, &requestMsg); err != nil {
		log.Printf("Trouble decoding request %v", err)
	}
	log.Printf("Received: %+v", requestMsg)
	//now respond
	if err := handleRequest(desc, handlerMap, conn, &requestMsg); err != nil {
		log.Printf("Trouble when responding to %+v response error %v", requestMsg, err)
		if err := bencode.Marshal(conn, ErrorResponse{
			Id:        requestMsg.Id,
			Status:    ErrorStat,
			ExMessage: err.Error(),
		}); err != nil {
			log.Printf("Trouble encoding error response %v", err)
		}
	}
	/*if err := bencode.Marshal(conn, fmt.Sprintf("hi %s", requestMsg.Op)); err != nil {
		log.Printf("Trouble encoding response %v", err)
	}*/

	log.Printf("Connection closed")
}

type Operation string

const (
	Describe Operation = "describe"
	Invoke   Operation = "invoke"
)

type Request struct {
	Id   string    `bencode:"id,omitempty"`
	Op   Operation `bencode:"op"`
	Var  string    `bencode:"var"`
	Args string    `bencode:"args"`
}

type DescribeResponse struct {
	Format     string      `bencode:"format"`
	Namespaces []Namespace `bencode:"namespaces"`
}

func (d *DescribeResponse) handlerMap() map[string]Handler {
	m := make(map[string]Handler)
	for _, ns := range d.Namespaces {
		for _, v := range ns.Vars {
			m[fmt.Sprintf("%s/%s", ns.Name, v.Name)] = v.Handler
		}
	}
	return m
}

type Namespace struct {
	Name string `bencode:"name"`
	Vars []Var  `bencode:"vars"`
}

type Var struct {
	Name    string  `bencode:"name"`
	Handler Handler `bencode:"-"`
}

type Status string

const (
	DoneStat  Status = "done"
	ErrorStat Status = "error"
)

type InvokeResponse struct {
	Id     string `bencode:"id"`
	Status Status `bencode:"status"`
	Value  string `bencode:"value"`
}

type ErrorResponse struct {
	Id        string `bencode:"id,omitempty"`
	Status    Status `bencode:"status"`
	ExMessage string `bencode:"ex-message"`
	ExData    string `bencode:"ex-data,omitempty"`
}

type Handler func(args []json.RawMessage) (json.RawMessage, error)

func handleRequest(desc *DescribeResponse, handlerMap map[string]Handler, w io.Writer, r *Request) error {
	switch r.Op {
	case Describe:
		return bencode.Marshal(w, *desc)
	case Invoke:
		h, ok := handlerMap[r.Var]
		if ok {
			var args []json.RawMessage
			if err := json.Unmarshal([]byte(r.Args), &args); err != nil {
				return err
			}
			if resp, err := h(args); err != nil {
				return err
			} else {
				iRes := InvokeResponse{
					Id:     r.Id,
					Status: DoneStat,
					Value:  string([]byte(resp)),
				}

				return bencode.Marshal(w, iRes)
			}
		} else {
			return fmt.Errorf("Var %s not found", r.Var)
		}
	default:
		return fmt.Errorf("Unexpected op %v", r.Op)
	}
}
