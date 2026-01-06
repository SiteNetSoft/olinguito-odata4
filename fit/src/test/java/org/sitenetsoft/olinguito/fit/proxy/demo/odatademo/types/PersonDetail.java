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
package org.sitenetsoft.olinguito.fit.proxy.demo.odatademo.types;

// CHECKSTYLE:OFF (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.EdmStreamValue;
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;

// CHECKSTYLE:ON (Maven checkstyle)

@Namespace("ODataDemo")
@EntityType(name = "PersonDetail",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface PersonDetail
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<PersonDetail>,
        StructuredQuery<PersonDetail> {

  @Key
  @Property(name = "PersonID",
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
  java.lang.Integer getPersonID();

  void setPersonID(java.lang.Integer _personID);

  @Property(name = "Age",
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
  java.lang.Short getAge();

  void setAge(java.lang.Short _age);

  @Property(name = "Gender",
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
  java.lang.Boolean getGender();

  void setGender(java.lang.Boolean _gender);

  @Property(name = "Phone",
      type = "Edm.String",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.String getPhone();

  void setPhone(java.lang.String _phone);

  @Property(name = "Address",
      type = "ODataDemo.Address",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  Address getAddress();

  void setAddress(Address _address);

  @Property(name = "Photo",
      type = "Edm.Stream",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  EdmStreamValue getPhoto();

  void setPhoto(EdmStreamValue _photo);

  @NavigationProperty(name = "Person",
      type = "ODataDemo.Person",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Persons",
      containsTarget = false)
  Person getPerson();

  void setPerson(Person _person);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "PersonID",
        type = "Edm.Int32")
    Annotatable getPersonIDAnnotations();

    @AnnotationsForProperty(name = "Age",
        type = "Edm.Byte")
    Annotatable getAgeAnnotations();

    @AnnotationsForProperty(name = "Gender",
        type = "Edm.Boolean")
    Annotatable getGenderAnnotations();

    @AnnotationsForProperty(name = "Phone",
        type = "Edm.String")
    Annotatable getPhoneAnnotations();

    @AnnotationsForProperty(name = "Address",
        type = "ODataDemo.Address")
    Annotatable getAddressAnnotations();

    @AnnotationsForProperty(name = "Photo",
        type = "Edm.Stream")
    Annotatable getPhotoAnnotations();

    @AnnotationsForNavigationProperty(name = "Person",
        type = "ODataDemo.Person")
    Annotatable getPersonAnnotations();
  }

}
