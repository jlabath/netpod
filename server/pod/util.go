package pod

import (
	"encoding/json"
	"fmt"
)

// DecodeArgs unmarshals a slice of JSON-encoded arguments into the provided target values.
//
// This function expects that the number of JSON arguments (`args`) matches the number of target
// values (`vals`). Each element in `args` is unmarshaled into the corresponding value in `vals`.
//
// If the number of arguments does not match the number of targets, or if there is an error
// during the unmarshaling process, an error is returned.
//
// Parameters:
// - args: A slice of `json.RawMessage` representing the JSON-encoded arguments to be decoded.
// - vals: A variadic list of pointers to the values into which each argument should be unmarshaled.
//
// Returns:
//   - An error if the number of arguments and targets do not match, or if any of the arguments fail
//     to unmarshal into their corresponding target.
//
// Example:
//
//	var val1 int
//	var val2 string
//	err := DecodeArgs([]json.RawMessage{json.RawMessage(`"42"`), json.RawMessage(`"hello"`)}, &val1, &val2)
//	if err != nil {
//	    log.Fatal(err)
//	}
//	fmt.Println(val1, val2) // Output: 42 hello
func DecodeArgs(args []json.RawMessage, vals ...interface{}) error {
	if len(args) != len(vals) {
		return fmt.Errorf("Invalid number of args passed %d to decode to %d targets", len(args), len(vals))
	}
	for idx, v := range vals {
		if err := json.Unmarshal(args[idx], v); err != nil {
			return fmt.Errorf("Trouble unmarshalling argument with index %d: %w", idx, err)
		}
	}
	return nil
}
