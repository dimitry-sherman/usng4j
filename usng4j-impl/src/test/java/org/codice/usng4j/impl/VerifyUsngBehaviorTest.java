package org.codice.usng4j.impl;

import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import org.codice.usng4j.BoundingBox;
import org.codice.usng4j.UtmUpsCoordinate;
import org.junit.Test;

public class VerifyUsngBehaviorTest {
  private static final List<LatLon> EXPECTED_13_234789 =
      ImmutableList.of(
          new LatLon(0.01, -108.0),
          new LatLon(8.0, -108.0),
          new LatLon(8.0, -102.0),
          new LatLon(0.01, -102.0));

  private static final List<LatLon> EXPECTED_13_234789_2100KM_S =
      ImmutableList.of(
          new LatLon(-72.0, -108.0),
          new LatLon(-64.0, -108.0),
          new LatLon(-64.0, -102.0),
          new LatLon(-72.0, -102.0));

  private static final List<LatLon> EXPECTED_A_2347891 =
      ImmutableList.of(
          new LatLon(-64.0, -186.0),
          new LatLon(-56.0, -186.0),
          new LatLon(-56.0, -180.0),
          new LatLon(-64.0, -180.0));

  private final CoordinateSystemTranslatorImpl translator = new CoordinateSystemTranslatorImpl();

  @Test(expected = IllegalArgumentException.class)
  public void testDirectPathUtmString() {
    // TODO - Usng4j does not find the following UTM string valid (fine due to ambiguity)
    actual("13 234789mE 234789mN");
  }

  @Test
  public void testDirectPathUtmStringWithTrailingNorth() {
    assertThat(actual("13 234789mE 234789mN N"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingSouth() {
    assertThat(actual("13 234789mE 2100000mN S"), is(EXPECTED_13_234789_2100KM_S));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingSouthNoNorthingBoundsCheck() {
    // TODO - Need better error handling in usng4j than just returning null
    assertThat(actual("13 234789mE 234789mN S"), is(nullValue()));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingNorthWithValidSouthmostLatBand() {
    assertThat(actual("13C 234789mE 234789mN N"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingSouthWithValidSouthmostLatBand() {
    assertThat(actual("13C 234789mE 2100000mN S"), is(EXPECTED_13_234789_2100KM_S));
  }

  @Test
  public void testDirectPathUtmStringOnlyWithValidSouthmostLatBand() {
    assertThat(actual("13C 234789mE 2100000mN"), is(equalTo(EXPECTED_13_234789_2100KM_S)));
  }

  @Test
  public void testDirectPathUtmStringOnlyWithValidNorthmostSouthLatBand() {
    assertThat(actual("13M 234789mE 2100000mN"), is(equalTo(EXPECTED_13_234789_2100KM_S)));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingNorthWithValidNorthmostLatBand() {
    assertThat(actual("13X 234789mE 234789mN N"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithTrailingSouthWithValidNorthmostLatBand() {
    // Resolves to north (lat band takes priority)
    assertThat(actual("13X 234789mE 234789mN S"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringOnlyWithValidNorthmostLatBand() {
    assertThat(actual("13X 234789mE 234789mN"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithAmbiguousLatBandNorth() {
    // Resolves to north
    assertThat(actual("13N 234789mE 234789mN"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithAmbiguousLatBandSouth() {
    // Resolves to north
    assertThat(actual("13S 234789mE 234789mN"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testDirectPathUtmStringWithExtraSpaces() {
    assertThat(actual("13N   234789mE   234789mN"), is(equalTo(EXPECTED_13_234789)));
  }

  @Test
  public void testUpsStringDisregardsNorthIndicator() {
    assertThat(actual("A 2347891mE 2347891mN N"), is(equalTo(EXPECTED_A_2347891)));
  }

  @Test
  public void testUpsStringDisregardsSouthIndicator() {
    assertThat(actual("A 2347891mE 2347891mN S"), is(equalTo(EXPECTED_A_2347891)));
  }

  private List<LatLon> actual(String coord) {
    testParamNotNull(coord);
    return Optional.of(coord)
        .map(this::parseUtmUpsString)
        .map(translator::toBoundingBox)
        .map(LatLon::fromBoundingBox)
        .orElse(null);
  }

  private UtmUpsCoordinate parseUtmUpsString(String input) {
    try {
      return translator.parseUtmUpsString(input);
    } catch (ParseException e) {
      throw new AssertionError("Could not parse expected coordinate string; " + e.getMessage());
    }
  }

  private static void testParamNotNull(Object testParam) {
    if (testParam == null) {
      fail("Provided test parameter cannot be null");
    }
  }

  private static class LatLon {
    private final Double lat;
    private final Double lon;

    LatLon(Double lat, Double lon) {
      this.lat = lat;
      this.lon = lon;
    }

    Double getLat() {
      return lat;
    }

    Double getLon() {
      return lon;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LatLon latLon = (LatLon) o;
      return lat.equals(latLon.lat) && lon.equals(latLon.lon);
    }

    @Override
    public int hashCode() {
      int result = lat.hashCode();
      result = 31 * result + lon.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "(" + getLat() + ", " + getLon() + ")";
    }

    static List<LatLon> fromBoundingBox(BoundingBox bb) {
      notNull(bb, "The provided bounding box cannot be null");
      return ImmutableList.of(
          new LatLon(bb.getSouth(), bb.getWest()),
          new LatLon(bb.getNorth(), bb.getWest()),
          new LatLon(bb.getNorth(), bb.getEast()),
          new LatLon(bb.getSouth(), bb.getEast()));
    }
  }
}
