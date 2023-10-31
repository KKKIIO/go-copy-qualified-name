package main

import "encoding/json"

func main() {
    _, _ := json.<caret>Marshal(nil)
}