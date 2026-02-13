# Maven Dependencies

### Consuming Olinguito OData 4.0 Library

##### Common Dependencies
The following common dependencies needs to be added to the pom.xml of the consuming project:

```xml
<dependency>
  <groupId>org.sitenetsoft.olinguito</groupId>
  <artifactId>odata-commons-api</artifactId>
  <version>${project.version}</version>
</dependency>
<dependency>
  <groupId>org.sitenetsoft.olinguito</groupId>
  <artifactId>odata-commons-core</artifactId>
  <version>${project.version}</version>
</dependency>
```


##### OData Client Dependencies
For realization of *OData-Client* projects additional dependencies are necessary:

```xml
<dependency>
  <groupId>org.sitenetsoft.olinguito</groupId>
  <artifactId>odata-server-api</artifactId>
  <version>${olinguito.version}</version>
  <scope>compile</scope>
</dependency>
<dependency>
  <groupId>org.sitenetsoft.olinguito</groupId>
  <artifactId>odata-server-core</artifactId>
  <version>${olinguito.version}</version>
  <scope>runtime</scope>
</dependency>
```


##### OData Client Dependencies
For realization of *OData-Server* projects additional dependencies are necessary:

```xml
<dependency>
 <groupId>org.sitenetsoft.olinguito</groupId>
 <artifactId>odata-server-api</artifactId>
 <version>${olinguito.version}</version>
 <scope>compile</scope>
</dependency>
<dependency>
 <groupId>org.sitenetsoft.olinguito</groupId>
 <artifactId>odata-server-core</artifactId>
 <version>${olinguito.version}</version>
 <scope>runtime</scope>
</dependency>
```


Whereas the variable `${olinguito.version}` has to be replaced with the concrete version of the library.
See also:

* [Download Page](download.md)
* [Maven Central](https://search.maven.org/#search|ga|1|org.sitenetsoft.olinguito)
