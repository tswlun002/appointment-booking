package capitec.branch.appointment.location.domain;

import org.apache.http.util.Asserts;

import java.util.Objects;

public record Coordinates(
        double latitude,
        double longitude
) {
    public Coordinates {
        Asserts.check(latitude >= -90 && latitude <= 90, "Latitude must be between -90 and 90");
        Asserts.check(longitude >= -180 && longitude <= 180, "Longitude must be between -180 and 180");
    }

    public double distanceTo(Coordinates other) {
        Asserts.notNull(other, "other coordinates");
        return haversineDistance(this.latitude, this.longitude, other.latitude, other.longitude);
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coordinates that)) return false;
        return Double.compare(latitude, that.latitude) == 0 && Double.compare(longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }
}

