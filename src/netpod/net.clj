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

(defn out-stream->in-stream
  "turns io.ByteArrayOutputStream to io.ByteArrayInputStream"
  [out-stream]
  (ByteArrayInputStream. (.toByteArray out-stream)))

(defn send-msg
  "writes data to unix socket and returns a response"
  [path msg]
  (with-open [channel (SocketChannel/open StandardProtocolFamily/UNIX)]
    (let [buffer (ByteBuffer/allocate 4)
          bytea (byte-array 1024)
          output-stream (ByteArrayOutputStream.)
          src (out-stream->in-stream (write-bencode (ByteArrayOutputStream.) msg))]
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
      (read-bencode (PushbackInputStream. (out-stream->in-stream output-stream))))))
