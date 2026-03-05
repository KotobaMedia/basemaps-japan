package com.protomaps.basemap.feature;

import static com.onthegomap.planetiler.TestUtils.newPoint;
import static org.junit.jupiter.api.Assertions.*;

import com.onthegomap.planetiler.reader.SimpleFeature;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class DisputedAreasTest {

  @Test
  void membershipInsideNorthernTerritories() {
    var feature = SimpleFeature.create(newPoint(146.2, 44.3), new HashMap<>(), "osm", null, 1);
    var membership = DisputedAreas.membership(feature);
    assertTrue(membership.northernTerritories());
    assertTrue(membership.inAnyDisputedRegion());
  }

  @Test
  void membershipInsideTakeshima() {
    var feature = SimpleFeature.create(newPoint(131.86, 37.24), new HashMap<>(), "osm", null, 1);
    var membership = DisputedAreas.membership(feature);
    assertTrue(membership.takeshima());
    assertTrue(membership.inAnyDisputedRegion());
  }

  @Test
  void membershipInsideSenkaku() {
    var feature = SimpleFeature.create(newPoint(124.558, 25.922), new HashMap<>(), "osm", null, 1);
    var membership = DisputedAreas.membership(feature);
    assertTrue(membership.senkaku());
    assertTrue(membership.inAnyDisputedRegion());
  }

  @Test
  void membershipOutsideDisputedAreas() {
    var feature = SimpleFeature.create(newPoint(139.7, 35.6), new HashMap<>(), "osm", null, 1);
    var membership = DisputedAreas.membership(feature);
    assertFalse(membership.inAnyDisputedRegion());
  }

  @Test
  void allowedLanduseKindsInNorthernTerritories() {
    assertTrue(DisputedAreas.isAllowedLanduseInNorthernTerritories("grassland"));
    assertTrue(DisputedAreas.isAllowedLanduseInNorthernTerritories("wood"));
    assertTrue(DisputedAreas.isAllowedLanduseInNorthernTerritories("wetland"));
    assertFalse(DisputedAreas.isAllowedLanduseInNorthernTerritories("park"));
  }
}
