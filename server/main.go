package main

import (
	"context"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"time"

	pod "github.com/jlabath/netpod/server/pod"
)

func greetHandler(ctx context.Context, encodedArgs []json.RawMessage) (json.RawMessage, error) {
	var name string
	if err := pod.DecodeArgs(encodedArgs, &name); err != nil {
		return nil, fmt.Errorf("greetHandler: %w", err)
	}

	if err := json.Unmarshal(encodedArgs[0], &name); err != nil {
		return nil, err
	}

	//sleep somewhat
	time.Sleep(time.Millisecond * time.Duration(rand.Intn(1000)+1))

	return json.Marshal(fmt.Sprintf("hi there %s", name))
}

func brokenHandler(ctx context.Context, encodedArgs []json.RawMessage) (json.RawMessage, error) {
	//sleep somewhat
	time.Sleep(time.Millisecond * time.Duration(rand.Intn(1000)+1))
	return nil, fmt.Errorf("this will fail")
}

func main() {
	//init rand num generator used in handlers
	rand.Seed(time.Now().UnixNano())

	if len(os.Args) < 2 {
		fmt.Fprintf(os.Stderr, "Missing a filepath argument for socket to listen on\n")
		os.Exit(1)
	}

	// socket file path is first argument given to program
	socketPath := os.Args[1]

	//describe response
	ds := pod.DescribeResponse{
		Format: "json",
		Namespaces: []pod.Namespace{pod.Namespace{
			Name: "sample.service",
			Vars: []pod.Var{pod.Var{
				Name:    "greet",
				Handler: greetHandler,
			},
				pod.Var{
					Name:    "broken-func",
					Handler: brokenHandler,
				}},
		}},
	}

	pod.Listen(socketPath, ds)
}
