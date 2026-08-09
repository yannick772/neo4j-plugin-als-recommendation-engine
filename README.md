# neo4j-plugin-als-recommendation-engine

---

## Import into Neo4j Docker 

Compile the Maven project and copy the generated `.jar` file into the `plugins` folder, it can be found under `var/lib/neo4j`

---

To import csv files, put them into the `import` folder, which can be found under `var/lib/neo4j`.

The command to import the movielens databases from csv is:

```cypher
CREATE CONSTRAINT FOR (u:User) REQUIRE u.userId IS UNIQUE
CREATE CONSTRAINT FOR (m:Movie) REQUIRE m.movieId IS UNIQUE
```

```cypher
LOAD CSV WITH HEADERS 
FROM "file:///users.csv" AS row 
CALL (row) {
MERGE (u:User {userId: toInteger(row.userId)})
} IN TRANSACTIONS OF 50000 ROWS;
```
10M: Braucht ungefähr 2 Minuten

```cypher
LOAD CSV WITH HEADERS 
FROM "file:///movies.csv" AS row 
CALL (row) {
MERGE (m:Movie {
  movieId: toInteger(row.movieId),
  title: row.title,
  genres: row.genres})
} IN TRANSACTIONS OF 50000 ROWS;
```
10M: Braucht ungefähr 2 Minuten

```cypher
LOAD CSV WITH HEADERS 
FROM "file:///ratings.csv" AS row 
CALL (row) {
MATCH (u:User {userId: toInteger(row.userId)})
MATCH (m:Movie {movieId: toInteger(row.movieId)})
CREATE (u)-[:RATED {
  rating: toInteger(row.rating),
  timestamp: toInteger(row.timestamp)
}]->(m)
} IN TRANSACTIONS OF 50000 ROWS;
```
1M:  Braucht ungefähr 2 Minuten
10M: Braucht ungefähr 10 Minuten
32M: Braucht ungefähr 34 Minuten

### Import via console

Use this command to test, whether your `.csv` files are valid

```shell
neo4j-admin database import full --dry-run=true --nodes=import/movies_header.csv,import/movies.csv --nodes=import/users_header.csv,import/users.csv --relationships=import/ratings_header.csv,import/ratings.csv
```

Use this command to import them

```shell
docker exec --interactive --tty <containerID/name> neo4j-admin database import full --nodes=import/movies_header.csv,import/movies.csv --nodes=import/users_header.csv,import/users.csv --relationships=import/ratings_header.csv,import/ratings.csv
```