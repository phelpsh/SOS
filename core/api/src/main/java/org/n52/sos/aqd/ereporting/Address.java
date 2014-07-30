/**
 * Copyright (C) 2012-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.aqd.ereporting;

import org.n52.sos.util.Reference;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.n52.sos.util.Nillable;

import com.google.common.base.Preconditions;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class Address {

    private final Collection<GeographicalName> adminUnits = new LinkedList<>();
    private final Collection<String> locatorDesignators = new LinkedList<>();
    private final Collection<GeographicalName> locatorNames = new LinkedList<>();
    private final Collection<Nillable<GeographicalName>> addressAreas = new LinkedList<>();
    private final Collection<Nillable<GeographicalName>> postNames = new LinkedList<>();
    private Nillable<String> postCode = Nillable.missing();
    private final Collection<Nillable<GeographicalName>> thoroughfares = new LinkedList<>();
    private Nillable<Reference> addressFeature = Nillable.missing();

    public Collection<GeographicalName> getAdminUnits() {
        return Collections.unmodifiableCollection(adminUnits);
    }

    public Address addAdminUnit(GeographicalName adminUnit) {
        this.adminUnits.add(Preconditions.checkNotNull(adminUnit));
        return this;
    }

    public Collection<String> getLocatorDesignators() {
        return Collections.unmodifiableCollection(locatorDesignators);
    }

    public Address addLocatorDesignator(String locatorDesignator) {
        this.locatorDesignators.add(Preconditions.checkNotNull(locatorDesignator));
        return this;
    }

    public Collection<GeographicalName> getLocatorNames() {
        return Collections.unmodifiableCollection(locatorNames);
    }

    public Address addLocatorName(GeographicalName locatorName) {
        this.locatorNames.add(Preconditions.checkNotNull(locatorName));
        return this;
    }

    public Collection<Nillable<GeographicalName>> getAddressAreas() {
        return Collections.unmodifiableCollection(addressAreas);
    }

    public Address addAddressArea(Nillable<GeographicalName> addressArea) {
        this.addressAreas.add(Preconditions.checkNotNull(addressArea));
        return this;
    }

    public Address addAddressArea(GeographicalName addressArea) {
        return addAddressArea(Nillable.of(addressArea));
    }

    public Collection<Nillable<GeographicalName>> getPostNames() {
        return Collections.unmodifiableCollection(postNames);
    }

    public Address addPostName(Nillable<GeographicalName> postName) {
        this.postNames.add(Preconditions.checkNotNull(postName));
        return this;
    }

    public Address addPostName(GeographicalName postName) {
        return addPostName(Nillable.of(postName));
    }

    public Nillable<String> getPostCode() {
        return postCode;
    }

    public Address setPostCode(Nillable<String> postCode) {
        this.postCode = postCode;
        return this;
    }

    public Address setPostCode(String postCode) {
        return setPostCode(Nillable.of(postCode));
    }

    public Collection<Nillable<GeographicalName>> getThoroughfares() {
        return Collections.unmodifiableCollection(thoroughfares);
    }

    public Address addThoroughfare(
                                   Nillable<GeographicalName> thoroughfare) {
        this.thoroughfares.add(Preconditions.checkNotNull(thoroughfare));
        return this;
    }

    public Address addThoroughfare(GeographicalName thoroughfare) {
        return addThoroughfare(Nillable.of(thoroughfare));
    }

    public Nillable<Reference> getAddressFeature() {
        return addressFeature;
    }

    public Address setAddressFeature(Nillable<Reference> addressFeature) {
        this.addressFeature = Preconditions.checkNotNull(addressFeature);
        return this;
    }

    public Address setAddressFeature(Reference addressFeature) {
        return setAddressFeature(Nillable.of(addressFeature));
    }

}
