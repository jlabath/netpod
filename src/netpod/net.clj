(ns netpod.net
  (:require [bencode.core :refer [write-bencode read-bencode]])
  (:import '[java.io ByteArrayOutputStream ByteArrayInputStream PushbackInputStream]
           '[java.net StandardProtocolFamily]
           '[java.nio ByteBuffer]
           '[java.nio.channels SocketChannel]))

(comment (defn create-unix-domain-socket-address
           "resolves socket address in current directory"
           [file-name]
           (let [cwd (System/getProperty "user.dir")
                 uri (java.net.URI. (format "file://%s/%s" cwd file-name))
                 p (Paths/get uri)]
             p)))

(defn path->socket-address
  "converts the given path to a unix domain socket addres"
  [path]
  (java.net.UnixDomainSocketAddress/of path))

(defn copy-niobuf-to-output-stream
  "will copy the contents of the nio byte buffer to output-stream using the provided byte array as an intermediary"
  [bytebuf output-stream bytea]
  (let [buf-size (count bytea)]
    (while (.hasRemaining bytebuf)
      (let [chunk-size (min buf-size (.remaining bytebuf))]
        (.get bytebuf bytea 0 chunk-size)
        (.write output-stream bytea 0 chunk-size)))))

(defn copy-input-stream-to-niobuf
  "will copy the contents of input-stream (as much as possible) to nio byte buffer using the provided byte array as intermediary"
  [input-stream bytebuf bytea]
  (let [length (min (count bytea) (.capacity bytebuf) (.available input-stream))]
    (.read input-stream bytea 0 length)
    (.put bytebuf bytea 0 length)))

(defn is-byte-array? [x]
  (instance? (Class/forName "[B") x))

(defn b-encode
  "encodes a thing to a byte array"
  [thing]
  (let [out (ByteArrayOutputStream.)]
    (write-bencode out thing)
    (.toByteArray out)))

(defn ba->str
  "recursively crawls structures and decodes any bytearrea to str using UTF-8"
  [thing]
  (let [charset "UTF-8"]
    (cond
      (is-byte-array? thing) (String. thing charset)
      (map? thing) (reduce-kv
                    (fn [acc k v]
                      (assoc acc k (ba->str v))) {} thing)
      (vector? thing) (vec (map ba->str thing))
      :else thing)))

(defn b-decode
  "decodes a byte array into a thing"
  [bytea]
  (let [in (PushbackInputStream. (ByteArrayInputStream. bytea))
        decoded-thing (read-bencode in)]
    (ba->str decoded-thing)))

(defn send-msg
  "writes data to unix socket and returns a response"
  [path msg]
  (with-open [channel (SocketChannel/open StandardProtocolFamily/UNIX)]
    (let [buffer (ByteBuffer/allocate 4096)
          bytea (byte-array 4096)
          output-stream (ByteArrayOutputStream.)
          src (ByteArrayInputStream. (b-encode msg))]
      (.reset output-stream)
      (.connect channel (path->socket-address path))
      (while (pos? (.available src))
        ;;ready for writing
        (.clear buffer)
        (copy-input-stream-to-niobuf src buffer bytea)
        (.flip buffer)
        (while (.hasRemaining buffer)
          (.write channel buffer)))
      ;;here we are done sending message
      ;;and we will read response from server
      (.clear buffer)
      (while (pos? (.read channel buffer))
        ;;set buffer to reading again
        (.flip buffer)
        ;; write to BAOS
        (copy-niobuf-to-output-stream buffer output-stream bytea)
        (.clear buffer))
      (b-decode (.toByteArray output-stream)))))
