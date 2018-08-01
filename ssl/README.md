# Generating SSL certificates

Typical usage:

```shell
make FILE=client
make FILE=server
```

Will generate CA certificate and signed client and server certificates.

More "low-level" usage:

```shell
make generate-ca-certificate
make generate-private-key FILE=client 
make sign FILE=client
```

# Connecting to a server

First generate *client* and *server* certificates. Then start a server with it's cert and make ca.crt a trusted certification authority.

After that you can:

```shell
./connect.sh client localhost:8600 < file_with_a_data_to_be_sent.dat
```
