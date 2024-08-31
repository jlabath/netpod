package pod

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"os"

	bencode "github.com/jackpal/bencode-go"
)

//this is how one can talk to this server
//echo "Hello, World!" | nc -U ./server.sock

// Listen sets up a listener on the specified Unix socket path and handles incoming connections.
// It uses the provided DescribeResponse to serve responses for requests.
//
// Parameters:
//   - socketPath: The path to the Unix domain socket on which to listen for incoming connections.
//   - ds: A DescribeResponse instance that provides data and functionality for handling incoming requests.
func Listen(socketPath string, ds DescribeResponse) {

	// Remove any existing socket file
	if err := os.Remove(socketPath); err != nil && !os.IsNotExist(err) {
		log.Fatalf("Failed to remove existing socket file: %v", err)
	}

	//craft handelr map for fast lookups
	handlerMap := ds.handlerMap()

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
		//log.Printf("Accepted connection %+v", conn)

		// Handle the connection
		go handleConnection(&ds, handlerMap, conn)
	}
}

func handleConnection(desc *DescribeResponse, handlerMap map[string]Handler, conn net.Conn) {
	defer conn.Close()
	var requestMsg Request
	if err := bencode.Unmarshal(conn, &requestMsg); err != nil {
		log.Printf("Trouble decoding request %v", err)
	}
	//log.Printf("Received: %+v", requestMsg)
	//now respond
	if err := handleRequest(desc, handlerMap, conn, &requestMsg); err != nil {
		//log.Printf("Trouble when responding to %+v response error %v", requestMsg, err)
		if err := bencode.Marshal(conn, ErrorResponse{
			Id:        requestMsg.Id,
			Status:    ErrorStat,
			ExMessage: err.Error(),
		}); err != nil {
			log.Printf("Trouble encoding error response %v", err)
		}
	}

	//log.Printf("Connection closed")
}

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
