# cassandra-http-seedprovider
HTTP based SeedProvider for Apache Cassandra

## Usage

1. Build the jar with Maven (mvn package)
2. Copy jar to Cassandra lib folder (for example: /usr/share/cassandra/lib/)
3. Change SeedProvider to org.apache.cassandra.locator.HttpSeedProvider
4. Add "urls" key/value to parameters

## Example config


    seed_provider:
        - class_name: org.apache.cassandra.locator.HttpSeedProvider
          parameters:
              - urls: "http://host1.example.com/seeds.txt,http://host2.example.com/seeds.txt"
