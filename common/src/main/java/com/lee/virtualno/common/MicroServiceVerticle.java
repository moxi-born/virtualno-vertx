package com.lee.virtualno.common;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.JDBCDataSource;
import io.vertx.servicediscovery.types.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MicroServiceVerticle extends AbstractVerticle {
  protected ServiceDiscovery discovery;
  protected Set<io.vertx.servicediscovery.Record> registeredRecords = new ConcurrentHashSet<>();

  @Override
  public void start() throws Exception {
    discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions().setBackendConfiguration(config()));
  }

  public Future<Void> publishHttpEndpoint(String name, String host, int port) {
    io.vertx.servicediscovery.Record record = HttpEndpoint.createRecord(name, host, port, "/");
    return publish(record);
  }

  public Future<Void> publishMessageSource(String name, String address, Class<?> contentClass) {
    io.vertx.servicediscovery.Record record = MessageSource.createRecord(name, address, contentClass);
    return publish(record);
  }

  public Future<Void> publishMessageSource(String name, String address) {
    io.vertx.servicediscovery.Record record = MessageSource.createRecord(name, address);
    return publish(record);
  }

  public Future<Void> publishEventBusService(String name, String address, Class<?> serviceClass) {
    io.vertx.servicediscovery.Record record = EventBusService.createRecord(name, address, serviceClass);
    return publish(record);
  }

  public Future<Void> publishDataSource(String name, JsonObject location, JsonObject metaData) {
    io.vertx.servicediscovery.Record record = JDBCDataSource.createRecord(name, location, metaData);
    return publish(record);
  }

  protected Future<Void> publish(io.vertx.servicediscovery.Record record) {
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
    for (io.vertx.servicediscovery.Record record : registeredRecords) {
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
