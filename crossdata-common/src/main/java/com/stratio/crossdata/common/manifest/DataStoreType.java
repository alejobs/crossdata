/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package com.stratio.crossdata.common.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataStoreType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataStoreType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Version" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="RequiredProperties" type="{}PropertiesType" minOccurs="0"/>
 *         &lt;element name="OptionalProperties" type="{}PropertiesType" minOccurs="0"/>
 *         &lt;element name="Behaviors" type="{}BehaviorsType" minOccurs="0"/>
 *         &lt;element name="Functions" type="{}DataStoreFunctionsType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataStoreType", propOrder = {
    "name",
    "version",
    "requiredProperties",
    "optionalProperties",
    "behaviors",
    "functions"
})
public class DataStoreType extends CrossdataManifest {

    private static final long serialVersionUID = -6186565647139757536L;
    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Version", required = true)
    protected String version;
    @XmlElement(name = "RequiredProperties")
    protected PropertiesType requiredProperties;
    @XmlElement(name = "OptionalProperties")
    protected PropertiesType optionalProperties;
    @XmlElement(name = "Behaviors")
    protected BehaviorsType behaviors;
    @XmlElement(name = "Functions")
    protected DataStoreFunctionsType functions;

    /**
     * Class constructor.
     */
    public DataStoreType() {
        super(TYPE_DATASTORE);
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the requiredProperties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesType }
     *     
     */
    public PropertiesType getRequiredProperties() {
        return requiredProperties;
    }

    /**
     * Sets the value of the requiredProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesType }
     *     
     */
    public void setRequiredProperties(PropertiesType value) {
        this.requiredProperties = value;
    }

    /**
     * Gets the value of the optionalProperties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesType }
     *     
     */
    public PropertiesType getOptionalProperties() {
        return optionalProperties;
    }

    /**
     * Sets the value of the optionalProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesType }
     *     
     */
    public void setOptionalProperties(PropertiesType value) {
        this.optionalProperties = value;
    }

    /**
     * Gets the value of the behaviors property.
     * 
     * @return
     *     possible object is
     *     {@link BehaviorsType }
     *     
     */
    public BehaviorsType getBehaviors() {
        return behaviors;
    }

    /**
     * Sets the value of the behaviors property.
     * 
     * @param value
     *     allowed object is
     *     {@link BehaviorsType }
     *     
     */
    public void setBehaviors(BehaviorsType value) {
        this.behaviors = value;
    }

    /**
     * Gets the value of the functions property.
     * 
     * @return
     *     possible object is
     *     {@link DataStoreFunctionsType }
     *     
     */
    public DataStoreFunctionsType getFunctions() {
        return functions;
    }

    /**
     * Sets the value of the functions property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataStoreFunctionsType }
     *     
     */
    public void setFunctions(DataStoreFunctionsType value) {
        this.functions = value;
    }

}
