package pod

import (
	"encoding/json"
	"fmt"
)

// Operation represents the type of operations this pod is able to respond to
type Operation string

const (
	//Describe is the operation that describes namespaces this pod makes available
	Describe Operation = "describe"
	//Invoke is the operation that invokes the handler specified in the request
	Invoke Operation = "invoke"
)

// Request is used by babashka client to communicate to this pod
type Request struct {
	Id   string    `bencode:"id,omitempty"`
	Op   Operation `bencode:"op"`
	Var  string    `bencode:"var"`
	Args string    `bencode:"args"`
}

// DescribeResponse is used to respond to Describe Request
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

// Namespace describing a unique namespace
type Namespace struct {
	Name string `bencode:"name"`
	Vars []Var  `bencode:"vars"`
}

// Var is the
type Var struct {
	Name    string  `bencode:"name"`
	Handler Handler `bencode:"-"`
}

// Status
type Status string

const (
	// DoneStat is used for successful responses
	DoneStat Status = "done"
	// ErrorStat is used to communicate error
	ErrorStat Status = "error"
)

// InvokeResponse is used to respond to invoke requests
type InvokeResponse struct {
	Id     string `bencode:"id"`
	Status Status `bencode:"status"`
	Value  string `bencode:"value"`
}

// ErrorResponse is used when a handler function returns an error
type ErrorResponse struct {
	Id        string `bencode:"id,omitempty"`
	Status    Status `bencode:"status"`
	ExMessage string `bencode:"ex-message"`
	ExData    string `bencode:"ex-data,omitempty"`
}

// Handler describes function to be used to serve requests
type Handler func(args []json.RawMessage) (json.RawMessage, error)
