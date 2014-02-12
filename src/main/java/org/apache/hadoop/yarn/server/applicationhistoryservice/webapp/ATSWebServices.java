/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.applicationhistoryservice.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.apptimeline.ATSEntities;
import org.apache.hadoop.yarn.api.records.apptimeline.ATSEntity;
import org.apache.hadoop.yarn.api.records.apptimeline.ATSEvents;
import org.apache.hadoop.yarn.api.records.apptimeline.ATSPutErrors;
import org.apache.hadoop.yarn.server.applicationhistoryservice.apptimeline.ApplicationTimelineReader.Field;
import org.apache.hadoop.yarn.server.applicationhistoryservice.apptimeline.ApplicationTimelineStore;
import org.apache.hadoop.yarn.server.applicationhistoryservice.apptimeline.NameValuePair;
import org.apache.hadoop.yarn.webapp.BadRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Path("/ws/v1/apptimeline")
//TODO: support XML serialization/deserialization
public class ATSWebServices {

  private static final Log LOG = LogFactory.getLog(ATSWebServices.class);

  private ApplicationTimelineStore store;

  @Inject
  public ATSWebServices(ApplicationTimelineStore store) {
    this.store = store;
  }

  @XmlRootElement(name = "about")
  @XmlAccessorType(XmlAccessType.NONE)
  @Public
  @Unstable
  public static class AboutInfo {

    private String about;

    public AboutInfo() {

    }

    public AboutInfo(String about) {
      this.about = about;
    }

    @XmlElement(name = "About")
    public String getAbout() {
      return about;
    }

    public void setAbout(String about) {
      this.about = about;
    }

  }

  /**
   * Return the description of the application timeline web services.
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public AboutInfo about(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res) {
    init(res);
    return new AboutInfo("Application Timeline API");
  }

  /**
   * Return a list of entities that match the given parameters.
   */
  @GET
  @Path("/{entityType}")
  @Produces({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public ATSEntities getEntities(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res,
      @PathParam("entityType") String entityType,
      @QueryParam("primaryFilter") String primaryFilter,
      @QueryParam("secondaryFilter") String secondaryFilter,
      @QueryParam("windowStart") String windowStart,
      @QueryParam("windowEnd") String windowEnd,
      @QueryParam("limit") String limit,
      @QueryParam("fields") String fields) {
    init(res);
    ATSEntities entities = null;
    try {
      entities = store.getEntities(
          parseStr(entityType),
          parseLongStr(limit),
          parseLongStr(windowStart),
          parseLongStr(windowEnd),
          parsePairStr(primaryFilter, ":"),
          parsePairsStr(secondaryFilter, ",", ":"),
          parseFieldsStr(fields, ","));
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          "windowStart, windowEnd or limit is not a numeric value.");
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("requested invalid field.");
    } catch (IOException e) {
      LOG.error("Error getting entities", e);
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (entities == null) {
      return new ATSEntities();
    }
    return entities;
  }

  /**
   * Return a single entity of the given entity type and Id.
   */
  @GET
  @Path("/{entityType}/{entityId}")
  @Produces({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public ATSEntity getEntity(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res,
      @PathParam("entityType") String entityType,
      @PathParam("entityId") String entityId,
      @QueryParam("fields") String fields) {
    init(res);
    ATSEntity entity = null;
    try {
      entity =
          store.getEntity(parseStr(entityId), parseStr(entityType),
              parseFieldsStr(fields, ","));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "requested invalid field.");
    } catch (IOException e) {
      LOG.error("Error getting entity", e);
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (entity == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return entity;
  }

  /**
   * Return the events that match the given parameters.
   */
  @GET
  @Path("/{entityType}/events")
  @Produces({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public ATSEvents getEvents(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res,
      @PathParam("entityType") String entityType,
      @QueryParam("entityId") String entityId,
      @QueryParam("eventType") String eventType,
      @QueryParam("windowStart") String windowStart,
      @QueryParam("windowEnd") String windowEnd,
      @QueryParam("limit") String limit) {
    init(res);
    ATSEvents events = null;
    try {
      events = store.getEntityTimelines(
          parseStr(entityType),
          parseArrayStr(entityId, ","),
          parseLongStr(limit),
          parseLongStr(windowStart),
          parseLongStr(windowEnd),
          parseArrayStr(eventType, ","));
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          "windowStart, windowEnd or limit is not a numeric value.");
    } catch (IOException e) {
      LOG.error("Error getting entity timelines", e);
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (events == null) {
      return new ATSEvents();
    }
    return events;
  }

  /**
   * Store the given entities into the timeline store, and return the errors
   * that happen during storing.
   */
  @POST
  @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public ATSPutErrors postEntities(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res,
      ATSEntities entities) {
    init(res);
    if (entities == null) {
      return new ATSPutErrors();
    }
    try {
      return store.put(entities);
    } catch (IOException e) {
      LOG.error("Error putting entities", e);
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private void init(HttpServletResponse response) {
    response.setContentType(null);
  }

  private static SortedSet<String> parseArrayStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    SortedSet<String> strSet = new TreeSet<String>();
    String[] strs = str.split(delimiter);
    for (String aStr : strs) {
      strSet.add(aStr.trim());
    }
    return strSet;
  }

  private static NameValuePair parsePairStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(delimiter, 2);
    return new NameValuePair(strs[0].trim(), strs[1].trim());
  }

  private static Collection<NameValuePair> parsePairsStr(
      String str, String aDelimiter, String pDelimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(aDelimiter);
    Set<NameValuePair> pairs = new HashSet<NameValuePair>();
    for (String aStr : strs) {
      pairs.add(parsePairStr(aStr, pDelimiter));
    }
    return pairs;
  }

  private static EnumSet<Field> parseFieldsStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(delimiter);
    List<Field> fieldList = new ArrayList<Field>();
    for (String s : strs) {
      s = s.trim().toUpperCase();
      if (s.equals("EVENTS"))
        fieldList.add(Field.EVENTS);
      else if (s.equals("LASTEVENTONLY"))
        fieldList.add(Field.LAST_EVENT_ONLY);
      else if (s.equals("RELATEDENTITIES"))
        fieldList.add(Field.RELATED_ENTITIES);
      else if (s.equals("PRIMARYFILTERS"))
        fieldList.add(Field.PRIMARY_FILTERS);
      else if (s.equals("OTHERINFO"))
        fieldList.add(Field.OTHER_INFO);
    }
    if (fieldList.size() == 0)
      return null;
    Field f1 = fieldList.remove(fieldList.size() - 1);
    if (fieldList.size() == 0)
      return EnumSet.of(f1);
    else
      return EnumSet.of(f1, fieldList.toArray(new Field[fieldList.size()]));
  }

  private static Long parseLongStr(String str) {
    return str == null ? null : Long.parseLong(str.trim());
  }

  private static String parseStr(String str) {
    return str == null ? null : str.trim();
  }

}
