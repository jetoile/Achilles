## Integration tests

 The integration tests use an embedded Cassandra server: [Cassandra Unit][cassandraUnit]
 
 To deactivate the embedded server and run the tests with a stand-alone Cassandra server, add the following VM argument:
 
> -DcassandraHost=&lt;host&gt;:&lt;port&gt;

[cassandraUnit]: https://github.com/jsevellec/cassandra-unit
 