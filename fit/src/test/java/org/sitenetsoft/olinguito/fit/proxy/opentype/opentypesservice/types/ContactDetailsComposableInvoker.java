/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sitenetsoft.olinguito.fit.proxy.opentype.opentypesservice.types;

// CHECKSTYLE:OFF (Maven checkstyle)

import org.sitenetsoft.olinguito.ext.proxy.api.StructuredComposableInvoker;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property;

public interface ContactDetailsComposableInvoker
    extends StructuredComposableInvoker<ContactDetails, ContactDetails.Operations>
{

  @Override
  ContactDetailsComposableInvoker select(String... select);

  @Override
  ContactDetailsComposableInvoker expand(String... expand);

  @Property(name = "FirstContacted",
      type = "Edm.Binary",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  byte[] getFirstContacted();

  void setFirstContacted(byte[] _firstContacted);

  @Property(name = "LastContacted",
      type = "Edm.DateTimeOffset",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.sql.Timestamp getLastContacted();

  void setLastContacted(java.sql.Timestamp _lastContacted);

  @Property(name = "Contacted",
      type = "Edm.Date",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.util.Calendar getContacted();

  void setContacted(java.util.Calendar _contacted);

  @Property(name = "GUID",
      type = "Edm.Guid",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.util.UUID getGUID();

  void setGUID(java.util.UUID _gUID);

  @Property(name = "PreferedContactTime",
      type = "Edm.TimeOfDay",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.util.Calendar getPreferedContactTime();

  void setPreferedContactTime(java.util.Calendar _preferedContactTime);

  @Property(name = "Byte",
      type = "Edm.Byte",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Short getByte();

  void setByte(java.lang.Short _byte);

  @Property(name = "SignedByte",
      type = "Edm.SByte",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Byte getSignedByte();

  void setSignedByte(java.lang.Byte _signedByte);

  @Property(name = "Double",
      type = "Edm.Double",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Double getDouble();

  void setDouble(java.lang.Double _double);

  @Property(name = "Single",
      type = "Edm.Single",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Float getSingle();

  void setSingle(java.lang.Float _single);

  @Property(name = "Short",
      type = "Edm.Int16",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Short getShort();

  void setShort(java.lang.Short _short);

  @Property(name = "Int",
      type = "Edm.Int32",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Integer getInt();

  void setInt(java.lang.Integer _int);

  @Property(name = "Long",
      type = "Edm.Int64",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Long getLong();

  void setLong(java.lang.Long _long);

}
