/**
 * Copyright (C) 2012-2015 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ogc.gml.time;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Test;
import org.n52.iceland.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.iceland.ogc.gml.time.TimeInstant;

/**
 * @since 4.0.0
 * 
 */
public class TimeInstantTest {

    @Test
    public void isEmptyForDefaultConstructorTest() {
        assertTrue("new TimeInstant is NOT empty", new TimeInstant().isEmpty());
    }

    @Test
    public void isEmptyForConstructorWithNullTimeTest() {
        assertTrue("new TimeInstant(null) is NOT empty", new TimeInstant((DateTime) null).isEmpty());
    }

    @Test
    public void isNotEmptyForConstructorWithTimeAndNullIndeterminateValueTest() {
        assertFalse("new TimeInstant(new DateTime()) is empty", new TimeInstant(new DateTime(), null).isEmpty());
    }

    @Test
    public void isNotEmptyForConstructorWithNullTimeAndIndeterminateValueTest() {
        assertFalse("new TimeInstant(null) is empty", new TimeInstant((TimeIndeterminateValue) null)
                .setSosIndeterminateTime(org.n52.iceland.ogc.ows.OWSConstants.ExtendedIndeterminateTime.latest).isEmpty());
    }
    
    @Test
    public void isNotEmptyForConstructorWithDate() {
        assertFalse("new TimeInstant(new DateTime()) is empty", new TimeInstant(new Date()).isEmpty());
    }

    @Test
    public void shouldEqualTime() {
        DateTime dateTime = new DateTime();
        TimeInstant timeInstant = new TimeInstant(dateTime);
        TimeInstant equalTimeInstant = new TimeInstant(dateTime);
        assertTrue("TimeInstants are NOT equal", timeInstant.equals(equalTimeInstant));
    }
        
    @Test
    public void shouldEqualIndeterminateValue() {
        TimeIndeterminateValue tiv = TimeIndeterminateValue.after;
        TimeInstant timeInstant = new TimeInstant(tiv);
        TimeInstant equalTimeInstant = new TimeInstant(tiv);
        assertTrue("TimeInstants are NOT equal", timeInstant.equals(equalTimeInstant));
    }
    
    @Test
    public void shouldEqualTimeAndIndeterminateValue() {
        DateTime dateTime = new DateTime();
        TimeIndeterminateValue tiv = TimeIndeterminateValue.after;
        TimeInstant timeInstant = new TimeInstant(dateTime, tiv);
        TimeInstant equalTimeInstant = new TimeInstant(dateTime, tiv);
        assertTrue("TimeInstants are NOT equal", timeInstant.equals(equalTimeInstant));
    }
    
    @Test
    public void shouldEqualSosIndeterminateValue() {
        org.n52.iceland.ogc.ows.OWSConstants.ExtendedIndeterminateTime sit = org.n52.iceland.ogc.ows.OWSConstants.ExtendedIndeterminateTime.first;
        TimeInstant timeInstant = new TimeInstant(sit);
        TimeInstant equalTimeInstant = new TimeInstant(sit);
        assertTrue("TimeInstants are NOT equal", timeInstant.equals(equalTimeInstant));
    }
    
    @Test
    public void testCompareTo() {
        TimeInstant timeInstantOne = new TimeInstant();
        TimeInstant timeInstantTwo = new TimeInstant();
        assertTrue("TimeInstants are equal", (timeInstantOne.compareTo(timeInstantTwo) == 0));
    }

}
