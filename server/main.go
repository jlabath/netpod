package main

import (
	"fmt"
	"log"
	"net"
	"os"

	bencode "github.com/jackpal/bencode-go"
)

//this is how one can talk to this server
//echo "Hello, World!" | nc -U ./server.sock

func main() {
	// Define the socket file path
	socketPath := "./server.sock"

	// Remove any existing socket file
	if err := os.Remove(socketPath); err != nil && !os.IsNotExist(err) {
		log.Fatalf("Failed to remove existing socket file: %v", err)
	}

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
		go handleConnection(conn)
	}
}

func handleConnection(conn net.Conn) {
	defer conn.Close()
	var requestMsg string
	if err := bencode.Unmarshal(conn, &requestMsg); err != nil {
		log.Printf("Trouble decoding request %v", err)
	}
	log.Printf("Received: %s", requestMsg)
	//now respond
	if err := bencode.Marshal(conn, fmt.Sprintf("hi %s", requestMsg)); err != nil {
		log.Printf("Trouble encoding response %v", err)
	}

	log.Printf("Connection closed")
}
