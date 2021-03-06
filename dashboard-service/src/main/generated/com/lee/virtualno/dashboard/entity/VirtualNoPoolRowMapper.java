package com.lee.virtualno.dashboard.entity;

/**
 * Mapper for {@link VirtualNoPool}.
 * NOTE: This class has been automatically generated from the {@link VirtualNoPool} original class using Vert.x codegen.
 */
@io.vertx.codegen.annotations.VertxGen
public interface VirtualNoPoolRowMapper extends io.vertx.sqlclient.templates.RowMapper<VirtualNoPool> {

  @io.vertx.codegen.annotations.GenIgnore
  VirtualNoPoolRowMapper INSTANCE = new VirtualNoPoolRowMapper() { };

  @io.vertx.codegen.annotations.GenIgnore
  java.util.stream.Collector<io.vertx.sqlclient.Row, ?, java.util.List<VirtualNoPool>> COLLECTOR = java.util.stream.Collectors.mapping(INSTANCE::map, java.util.stream.Collectors.toList());

  @io.vertx.codegen.annotations.GenIgnore
  default VirtualNoPool map(io.vertx.sqlclient.Row row) {
    VirtualNoPool obj = new VirtualNoPool();
    Object val;
    int idx;
    if ((idx = row.getColumnIndex("created_by")) != -1 && (val = row.getString(idx)) != null) {
      obj.setCreatedBy((java.lang.String)val);
    }
    if ((idx = row.getColumnIndex("created_date")) != -1 && (val = row.getLocalDateTime(idx)) != null) {
      obj.setCreatedDate((java.time.LocalDateTime)val);
    }
    if ((idx = row.getColumnIndex("description")) != -1 && (val = row.getString(idx)) != null) {
      obj.setDescription((java.lang.String)val);
    }
    if ((idx = row.getColumnIndex("enable")) != -1 && (val = row.getBoolean(idx)) != null) {
      obj.setEnable((boolean)val);
    }
    if ((idx = row.getColumnIndex("serial_number")) != -1 && (val = row.getString(idx)) != null) {
      obj.setSerialNumber((java.lang.String)val);
    }
    if ((idx = row.getColumnIndex("updated_by")) != -1 && (val = row.getString(idx)) != null) {
      obj.setUpdatedBy((java.lang.String)val);
    }
    if ((idx = row.getColumnIndex("updated_date")) != -1 && (val = row.getLocalDateTime(idx)) != null) {
      obj.setUpdatedDate((java.time.LocalDateTime)val);
    }
    return obj;
  }
}
