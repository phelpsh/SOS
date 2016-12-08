/*
 * Copyright (C) 2012-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.encode;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.opengis.om.x20.OMObservationType;
import net.opengis.waterml.x20.DefaultTVPMeasurementMetadataDocument;
import net.opengis.waterml.x20.MeasureTVPType;
import net.opengis.waterml.x20.MeasurementTimeseriesDocument;
import net.opengis.waterml.x20.MeasurementTimeseriesType;
import net.opengis.waterml.x20.TVPDefaultMetadataPropertyType;
import net.opengis.waterml.x20.TVPMeasurementMetadataType;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.shetland.ogc.SupportedType;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.AbstractObservationValue;
import org.n52.shetland.ogc.om.MultiObservationValues;
import org.n52.shetland.ogc.om.ObservationType;
import org.n52.shetland.ogc.om.ObservationValue;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.SingleObservationValue;
import org.n52.shetland.ogc.om.TimeValuePair;
import org.n52.shetland.ogc.om.values.CountValue;
import org.n52.shetland.ogc.om.values.QuantityValue;
import org.n52.shetland.ogc.om.values.TVPValue;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.wml.WaterMLConstants;
import org.n52.shetland.util.CollectionHelper;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.sos.coding.encode.EncodingValues;
import org.n52.sos.encode.streaming.WmlTVPEncoderv20XmlStreamWriter;
import org.n52.sos.ogc.wml.ConformanceClassesWML2;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.util.CodingHelper;
import org.n52.svalbard.EncodingContext;
import org.n52.svalbard.encode.EncoderKey;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.exception.UnsupportedEncoderInputException;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Encoder class for WaterML 2.0 TimeseriesValuePair (TVP)
 *
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 * @since 4.0.0
 *
 */
public class WmlTVPEncoderv20 extends AbstractWmlEncoderv20 {

    private static final Logger LOGGER = LoggerFactory.getLogger(WmlTVPEncoderv20.class);

    // TODO: change to correct conformance class
    private static final Set<String> CONFORMANCE_CLASSES = Sets.newHashSet(
            ConformanceClassesWML2.UML_MEASUREMENT_TIMESERIES_TVP_OBSERVATION,
            ConformanceClassesWML2.UML_TIMESERIES_TVP_OBSERVATION,
            ConformanceClassesWML2.UML_MEASUREMENT_TIMESERIES_TVP_OBSERVATION,
            ConformanceClassesWML2.XSD_XML_RULES,
            ConformanceClassesWML2.XSD_TIMESERIES_OBSERVATION,
            ConformanceClassesWML2.XSD_TIMESERIES_TVP_OBSERVATION,
            ConformanceClassesWML2.XSD_MEASUREMENT_TIMESERIES_TVP);

    private static final ImmutableSet<SupportedType> SUPPORTED_TYPES
            = ImmutableSet.<SupportedType>builder()
                    .add(new ObservationType(WaterMLConstants.OBSERVATION_TYPE_MEASURMENT_TVP))
                    .build();

    private static final Set<EncoderKey> ENCODER_KEYS = createEncoderKeys();

    private static final Map<String, Map<String, Set<String>>> SUPPORTED_RESPONSE_FORMATS = Collections.singletonMap(
            SosConstants.SOS,
            Collections.singletonMap(Sos2Constants.SERVICEVERSION, Collections.singleton(WaterMLConstants.NS_WML_20)));

    public WmlTVPEncoderv20() {
        LOGGER.debug("Encoder for the following keys initialized successfully: {}!", Joiner.on(", ")
                .join(ENCODER_KEYS));
    }


    @Override
    public Set<EncoderKey> getKeys() {
        return Collections.unmodifiableSet(ENCODER_KEYS);
    }

    @Override
    public Set<SupportedType> getSupportedTypes() {
        return Collections.unmodifiableSet(SUPPORTED_TYPES);
    }

    @Override
    public Set<String> getConformanceClasses(String service, String version) {
        if(SosConstants.SOS.equals(service) && Sos2Constants.SERVICEVERSION.equals(version)) {
            return Collections.unmodifiableSet(CONFORMANCE_CLASSES);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedResponseFormats(String service, String version) {
        return SUPPORTED_RESPONSE_FORMATS.getOrDefault(service, Collections.emptyMap())
                .getOrDefault(version, Collections.emptySet());
    }

    @Override
    public Map<String, Set<String>> getSupportedResponseFormatObservationTypes() {
        return Collections.singletonMap(WaterMLConstants.NS_WML_20,
                (Set<String>) getSupportedTypes()
                    .stream()
                    .filter(sp -> sp instanceof ObservationType)
                    .map(sp -> ((ObservationType)sp).getValue())
                    .collect(Collectors.toSet()));
    }

    @Override
    public Set<SchemaLocation> getSchemaLocations() {
        return Sets.newHashSet(WaterMLConstants.WML_20_SCHEMA_LOCATION, WaterMLConstants.WML_20_TS_SCHEMA_LOCATION);
    }

    @Override
    public boolean supportsResultStreamingForMergedValues() {
        return true;
    }

    @Override
    public XmlObject encode(Object element, EncodingContext additionalValues) throws EncodingException,
            UnsupportedEncoderInputException {
        XmlObject encodedObject = null;
        if (element instanceof ObservationValue) {
            encodedObject = encodeResult((ObservationValue<?>)element);
        } else {
            encodedObject = super.encode(element, additionalValues);
        }
        return encodedObject;
    }

    @Override
    public void encode(Object objectToEncode, OutputStream outputStream, EncodingValues encodingValues)
            throws EncodingException {
        encodingValues.setEncoder(this);
        if (objectToEncode instanceof OmObservation) {
            try {
                new WmlTVPEncoderv20XmlStreamWriter().write((OmObservation)objectToEncode, outputStream, encodingValues);
            } catch (XMLStreamException xmlse) {
                throw new EncodingException("Error while writing element to stream!", xmlse);
            }
        } else {
            super.encode(objectToEncode, outputStream, encodingValues);
        }
    }

    @Override
    protected XmlObject createResult(OmObservation sosObservation) throws EncodingException {
        return createMeasurementTimeseries(sosObservation);
    }

    @Override
    protected XmlObject encodeResult(ObservationValue<?> observationValue) throws EncodingException {
        return createMeasurementTimeseries((AbstractObservationValue<?>)observationValue);
    }

    @Override
    protected void addObservationType(OMObservationType xbObservation, String observationType) {
        if (!Strings.isNullOrEmpty(observationType)) {
            if (observationType.equals(OmConstants.OBS_TYPE_MEASUREMENT)
                    || observationType.equals(WaterMLConstants.OBSERVATION_TYPE_MEASURMENT_TVP)) {
                xbObservation.addNewType().setHref(WaterMLConstants.OBSERVATION_TYPE_MEASURMENT_TVP);
            } else if (observationType.equals(OmConstants.OBS_TYPE_CATEGORY_OBSERVATION)
                    || observationType.equals(WaterMLConstants.OBSERVATION_TYPE_CATEGORICAL_TVP)) {
                xbObservation.addNewType().setHref(WaterMLConstants.OBSERVATION_TYPE_CATEGORICAL_TVP);
            }
        }
    }

    /**
     * Create a XML MeasurementTimeseries object from SOS observation for
     * om:result
     *
     * @param sosObservation
     *            SOS observation
     * @return XML MeasurementTimeseries object
     */
    private XmlObject createMeasurementTimeseries(OmObservation sosObservation) {
        MeasurementTimeseriesDocument measurementTimeseriesDoc = MeasurementTimeseriesDocument.Factory.newInstance();
        MeasurementTimeseriesType measurementTimeseries = measurementTimeseriesDoc.addNewMeasurementTimeseries();
        measurementTimeseries.setId("timeseries." + sosObservation.getObservationID());
        measurementTimeseries.addNewMetadata().addNewTimeseriesMetadata().addNewTemporalExtent()
                .setHref("#" + sosObservation.getPhenomenonTime().getGmlId());

        TVPDefaultMetadataPropertyType xbMetaComponent = measurementTimeseries.addNewDefaultPointMetadata();

        DefaultTVPMeasurementMetadataDocument xbDefMeasureMetaComponent =
                DefaultTVPMeasurementMetadataDocument.Factory.newInstance();
        TVPMeasurementMetadataType defaultTVPMeasurementMetadata =
                xbDefMeasureMetaComponent.addNewDefaultTVPMeasurementMetadata();
        defaultTVPMeasurementMetadata.addNewInterpolationType().setHref(
                "http://www.opengis.net/def/timeseriesType/WaterML/2.0/continuous");

        xbDefMeasureMetaComponent.getDefaultTVPMeasurementMetadata().getInterpolationType().setTitle("Instantaneous");
        String unit = null;
        if (sosObservation.getValue() instanceof SingleObservationValue) {
            // time periods can not be set in MeasureTVPType
            if (sosObservation.getValue().getPhenomenonTime() instanceof TimeInstant) {
                SingleObservationValue<?> singleObservationValue = (SingleObservationValue<?>) sosObservation.getValue();
                String time = getTimeString(singleObservationValue.getPhenomenonTime());
                unit = singleObservationValue.getValue().getUnit();
                if (sosObservation.getValue().getValue() instanceof QuantityValue) {
                    QuantityValue quantityValue = (QuantityValue) singleObservationValue.getValue();
                    if (!quantityValue.getValue().equals(Double.NaN)) {
                        String value = Double.toString(quantityValue.getValue());
                        addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time, value);
                    }
                } else if (sosObservation.getValue().getValue() instanceof CountValue) {
                    CountValue countValue = (CountValue) singleObservationValue.getValue();
                    if (countValue.getValue() != null) {
                        String value = Integer.toString(countValue.getValue());
                        addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time, value);
                    }
                }
            }
        } else if (sosObservation.getValue() instanceof MultiObservationValues) {
            MultiObservationValues<?> observationValue = (MultiObservationValues<?>) sosObservation.getValue();
            TVPValue tvpValue = (TVPValue) observationValue.getValue();
            List<TimeValuePair> timeValuePairs = tvpValue.getValue();
            unit = tvpValue.getUnit();
            for (TimeValuePair timeValuePair : timeValuePairs) {
                // time periods can not be set in MeasureTVPType
                if (timeValuePair.getTime() instanceof TimeInstant) {
                    if (timeValuePair.getValue() instanceof QuantityValue) {
                        QuantityValue quantityValue = (QuantityValue) timeValuePair.getValue();
                        if (!quantityValue.getValue().equals(Double.NaN)) {
                            timeValuePair.getTime();
                            String time = getTimeString(timeValuePair.getTime());
                            String value = Double.toString(quantityValue.getValue());
                            addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time,
                                    value);
                        }
                    } else if (timeValuePair.getValue() instanceof CountValue) {
                        CountValue countValue = (CountValue) timeValuePair.getValue();
                        if (countValue.getValue() != null) {
                            String time = getTimeString(timeValuePair.getTime());
                            String value = Integer.toString(countValue.getValue());
                            addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time,
                                    value);
                        }
                    }
                }
            }
        }
        // set uom
        if (unit != null && !unit.isEmpty()) {
            defaultTVPMeasurementMetadata.addNewUom().setCode(unit);
        } else {
            OmObservableProperty observableProperty =
                    (OmObservableProperty) sosObservation.getObservationConstellation().getObservableProperty();
            if (observableProperty.isSetUnit()) {
                defaultTVPMeasurementMetadata.addNewUom().setCode(observableProperty.getUnit());
            }
        }

        xbMetaComponent.set(xbDefMeasureMetaComponent);
        return measurementTimeseriesDoc;
    }

    /**
     * Add a time an value to MeasureTVPType
     *
     * @param measurementTVP
     *            MeasureTVPType XML object
     * @param time
     *            Time a string
     * @param value
     *            value as string
     */
    private void addValuesToMeasurementTVP(MeasureTVPType measurementTVP, String time, String value) {
        measurementTVP.addNewTime().setStringValue(time);
        if (value != null && !value.isEmpty()) {
            measurementTVP.addNewValue().setStringValue(value);
        } else {
            measurementTVP.addNewValue().setNil();
            measurementTVP.addNewMetadata().addNewTVPMeasurementMetadata().addNewNilReason().setNilReason("missing");
        }
    }

    private XmlObject createMeasurementTimeseries(AbstractObservationValue<?> observationValue)  {
        MeasurementTimeseriesDocument measurementTimeseriesDoc = MeasurementTimeseriesDocument.Factory.newInstance();
        MeasurementTimeseriesType measurementTimeseries = measurementTimeseriesDoc.addNewMeasurementTimeseries();
        measurementTimeseries.setId("timeseries." + observationValue.getObservationID());
        measurementTimeseries.addNewMetadata().addNewTimeseriesMetadata().addNewTemporalExtent()
                .setHref("#" + observationValue.getPhenomenonTime().getGmlId());

        TVPDefaultMetadataPropertyType xbMetaComponent = measurementTimeseries.addNewDefaultPointMetadata();

        DefaultTVPMeasurementMetadataDocument xbDefMeasureMetaComponent =
                DefaultTVPMeasurementMetadataDocument.Factory.newInstance();
        TVPMeasurementMetadataType defaultTVPMeasurementMetadata =
                xbDefMeasureMetaComponent.addNewDefaultTVPMeasurementMetadata();
        defaultTVPMeasurementMetadata.addNewInterpolationType().setHref(
                "http://www.opengis.net/def/timeseriesType/WaterML/2.0/continuous");

        xbDefMeasureMetaComponent.getDefaultTVPMeasurementMetadata().getInterpolationType().setTitle("Instantaneous");
        String unit = null;
        if (observationValue instanceof SingleObservationValue) {
            SingleObservationValue<?> singleObservationValue = (SingleObservationValue<?>) observationValue;
            String time = getTimeString(singleObservationValue.getPhenomenonTime());
            unit = singleObservationValue.getValue().getUnit();
            if (observationValue.getValue() instanceof QuantityValue) {
                QuantityValue quantityValue = (QuantityValue) singleObservationValue.getValue();
                if (!quantityValue.getValue().equals(Double.NaN)) {
                    String value = Double.toString(quantityValue.getValue());
                    addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time, value);
                }
            } else if (observationValue.getValue() instanceof CountValue) {
                CountValue countValue = (CountValue) singleObservationValue.getValue();
                if (countValue.getValue() != null) {
                    String value = Integer.toString(countValue.getValue());
                    addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time, value);
                }
            }
        } else if (observationValue instanceof MultiObservationValues) {
            MultiObservationValues<?> multiObservationValue = (MultiObservationValues<?>) observationValue;
            TVPValue tvpValue = (TVPValue) multiObservationValue.getValue();
            List<TimeValuePair> timeValuePairs = tvpValue.getValue();
            unit = tvpValue.getUnit();
            for (TimeValuePair timeValuePair : timeValuePairs) {
                if (timeValuePair.getValue() instanceof QuantityValue) {
                    QuantityValue quantityValue = (QuantityValue) timeValuePair.getValue();
                    if (!quantityValue.getValue().equals(Double.NaN)) {
                        timeValuePair.getTime();
                        String time = getTimeString(timeValuePair.getTime());
                        String value = Double.toString(quantityValue.getValue());
                        addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time,
                                value);
                    }
                } else if (timeValuePair.getValue() instanceof CountValue) {
                    CountValue countValue = (CountValue) timeValuePair.getValue();
                    if (countValue.getValue() != null) {
                        String time = getTimeString(timeValuePair.getTime());
                        String value = Integer.toString(countValue.getValue());
                        addValuesToMeasurementTVP(measurementTimeseries.addNewPoint().addNewMeasurementTVP(), time,
                                value);
                    }
                }
            }
        }
        // set uom
        if (unit != null && !unit.isEmpty()) {
            defaultTVPMeasurementMetadata.addNewUom().setCode(unit);
//        } else {
//            OmObservableProperty observableProperty =
//                    (OmObservableProperty) sosObservation.getObservationConstellation().getObservableProperty();
//            if (observableProperty.isSetUnit()) {
//                defaultTVPMeasurementMetadata.addNewUom().setCode(observableProperty.getUnit());
//            }
        }

        xbMetaComponent.set(xbDefMeasureMetaComponent);
        return measurementTimeseriesDoc;
    }
    @SuppressWarnings("unchecked")
    private static Set<EncoderKey> createEncoderKeys() {
        return CollectionHelper.union(getDefaultEncoderKeys(), CodingHelper.encoderKeysForElements(
                WaterMLConstants.NS_WML_20, GetObservationResponse.class, OmObservation.class,
                                                                          SingleObservationValue.class, MultiObservationValues.class));
    }
}
