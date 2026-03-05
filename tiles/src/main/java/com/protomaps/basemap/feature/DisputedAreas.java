package com.protomaps.basemap.feature;

import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.geojson.GeoJson;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

public class DisputedAreas {

  private DisputedAreas() {}

  private enum Area {
    NORTHERN_TERRITORIES("northern_territories"),
    TAKESHIMA("takeshima"),
    SENKAKU("senkaku");

    private final String id;

    Area(String id) {
      this.id = id;
    }

    static Area fromId(String id) {
      for (var area : Area.values()) {
        if (area.id.equals(id)) {
          return area;
        }
      }
      return null;
    }
  }

  private record AreaGeometry(PreparedGeometry geometry, Envelope envelope) {}

  public record Membership(boolean northernTerritories, boolean takeshima, boolean senkaku) {
    public static final Membership NONE = new Membership(false, false, false);

    public boolean inAnyDisputedRegion() {
      return northernTerritories || takeshima || senkaku;
    }

    public boolean inTakeshimaOrSenkaku() {
      return takeshima || senkaku;
    }
  }

  private static final Map<Area, AreaGeometry> AREAS = loadAreas();

  public static Membership membership(SourceFeature sf) {
    try {
      Geometry geometry = sf.latLonGeometry();
      Envelope envelope = geometry.getEnvelopeInternal();
      return new Membership(
        intersects(Area.NORTHERN_TERRITORIES, geometry, envelope),
        intersects(Area.TAKESHIMA, geometry, envelope),
        intersects(Area.SENKAKU, geometry, envelope)
      );
    } catch (GeometryException e) {
      return Membership.NONE;
    }
  }

  public static boolean isAllowedLanduseInNorthernTerritories(String kind) {
    return "grassland".equals(kind) || "wood".equals(kind) || "wetland".equals(kind);
  }

  private static boolean intersects(Area area, Geometry geometry, Envelope envelope) {
    AreaGeometry areaGeometry = AREAS.get(area);
    if (areaGeometry == null || !areaGeometry.envelope().intersects(envelope)) {
      return false;
    }
    return areaGeometry.geometry().intersects(geometry);
  }

  private static Map<Area, AreaGeometry> loadAreas() {
    EnumMap<Area, AreaGeometry> result = new EnumMap<>(Area.class);
    PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();

    GeoJson geojson = GeoJson.from(() -> {
      InputStream stream = DisputedAreas.class.getResourceAsStream("/disputed-areas.geojson");
      if (stream == null) {
        throw new FileNotFoundException("Missing resource /disputed-areas.geojson");
      }
      return stream;
    });

    for (var feature : geojson) {
      Object id = feature.tags().get("id");
      if (!(id instanceof String areaId)) {
        continue;
      }
      Area area = Area.fromId(areaId);
      if (area == null) {
        continue;
      }
      Geometry geometry = feature.geometry();
      result.put(area, new AreaGeometry(preparedGeometryFactory.create(geometry), geometry.getEnvelopeInternal()));
    }

    for (Area area : Area.values()) {
      if (!result.containsKey(area)) {
        throw new IllegalStateException("Missing disputed area geometry: " + area.id);
      }
    }

    return result;
  }
}
