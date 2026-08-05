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
package org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types;

// CHECKSTYLE:OFF (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;

// CHECKSTYLE:ON (Maven checkstyle)

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "CreditRecord",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface CreditRecord
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<CreditRecord>,
        StructuredQuery<CreditRecord> {

  @Key
  @Property(name = "CreditRecordID",
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
  java.lang.Integer getCreditRecordID();

  void setCreditRecordID(java.lang.Integer _creditRecordID);

  @Property(name = "IsGood",
      type = "Edm.Boolean",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Boolean getIsGood();

  void setIsGood(java.lang.Boolean _isGood);

  @Property(name = "Reason",
      type = "Edm.String",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.String getReason();

  void setReason(java.lang.String _reason);

  @Property(name = "CreatedDate",
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
  java.sql.Timestamp getCreatedDate();

  void setCreatedDate(java.sql.Timestamp _createdDate);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "CreditRecordID",
        type = "Edm.Int32")
    Annotatable getCreditRecordIDAnnotations();

    @AnnotationsForProperty(name = "IsGood",
        type = "Edm.Boolean")
    Annotatable getIsGoodAnnotations();

    @AnnotationsForProperty(name = "Reason",
        type = "Edm.String")
    Annotatable getReasonAnnotations();

    @AnnotationsForProperty(name = "CreatedDate",
        type = "Edm.DateTimeOffset")
    Annotatable getCreatedDateAnnotations();

  }

}
