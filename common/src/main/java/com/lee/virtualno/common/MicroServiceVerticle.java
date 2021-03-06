package com.lee.virtualno.common;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.MessageSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MicroServiceVerticle extends AbstractVerticle {
  public static final String CONFIG_APP_SQL_QUERIES_RESOURCE_FILE = "app.sqlqueries.resource.file";
  protected ServiceDiscovery discovery;
  protected Set<Record> registeredRecords = new ConcurrentHashSet<>();
  protected HashMap<String, String> sqlQueries;

  @Override
  public void start() throws Exception {
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    DatabindCodec.prettyMapper().registerModule(new JavaTimeModule());
    discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
      .setBackendConfiguration(
        new JsonObject()
          .put("connection", "127.0.0.1:2181")
          .put("ephemeral", true)
          .put("guaranteed", true)
          .put("basePath", "/services")
      ));
    sqlQueries = loadSqlQueries();
  }

  private HashMap<String, String> loadSqlQueries() throws IOException {
    String queriesFile = config().getString(CONFIG_APP_SQL_QUERIES_RESOURCE_FILE);
    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = Files.newInputStream(Paths.get(queriesFile));
    } else {
      queriesInputStream = getClass().getResourceAsStream("/sql.properties");
    }

    Properties queriesProps = new Properties();
    queriesProps.load(queriesInputStream);
    assert queriesInputStream != null;
    queriesInputStream.close();

    HashMap<String, String> sqlQueries = new HashMap<>();
    Set<String> enumeration = queriesProps.stringPropertyNames();
    enumeration.forEach(key -> {
      String value = queriesProps.getProperty(key);
      sqlQueries.put(key, value);
    });
    return sqlQueries;
  }

  public Future<Void> publishHttpEndpoint(String name, String host, int port) {
    Record record = HttpEndpoint.createRecord(name, host, port, "/");
    return publish(record);
  }

  public Future<Void> publishMessageSource(String name, String address, Class<?> contentClass) {
    Record record = MessageSource.createRecord(name, address, contentClass);
    return publish(record);
  }

  public Future<Void> publishMessageSource(String name, String address) {
    Record record = MessageSource.createRecord(name, address);
    return publish(record);
  }

  public Future<Void> publishEventBusService(String name, String address, Class<?> serviceClass) {
    Record record = EventBusService.createRecord(name, address, serviceClass);
    return publish(record);
  }

  protected Future<Void> publish(Record record) {
    Promise<Void> publishResult = Promise.promise();
    if (discovery == null) {
      try {
        start();
      } catch (Exception e) {
        throw new RuntimeException("Cannot create discovery service");
      }
    }

    discovery.publish(record)
      .onSuccess(success -> {
        registeredRecords.add(record);
        publishResult.complete();
      })
      .onFailure(publishResult::fail);
    return publishResult.future();
  }

  @Override
  public void stop(Promise<Void> promise) throws Exception {
    List<Promise<Void>> promises = new ArrayList<>();
    for (Record record : registeredRecords) {
      Promise<Void> unRegistrationPromise = Promise.promise();
      promises.add(unRegistrationPromise);
      discovery.unpublish(record.getRegistration(), unRegistrationPromise);
    }
    if(promises.isEmpty()) {
      discovery.close();
      promise.complete();
    } else {
      CompositeFuture composite = CompositeFuture.all(promises.stream().map(Promise::future).collect(Collectors.toList()));
      composite.onComplete(ar -> {
        discovery.close();
        if(ar.succeeded()) {
          promise.complete();
        } else {
          promise.fail(ar.cause());
        }
      });
    }
  }
}
