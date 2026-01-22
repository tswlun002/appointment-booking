package capitec.branch.appointment.location.domain;

import org.apache.http.util.Asserts;

import java.util.Objects;

public class BranchLocation {

    private final String branchCode;
    private final String branchId;
    private final String name;
    private final Coordinates coordinates;
    private final BranchAddress address;
    private final OperatingHours operatingHours;
    private final boolean businessBankCenter;
    private final boolean isClosed;

    private BranchLocation(String branchCode,String branchId, String name, Coordinates coordinates,
                           BranchAddress address, OperatingHours operatingHours,
                            boolean businessBankCenter, boolean isClosed) {
        this.branchCode = branchCode;
        this.branchId = branchId;
        this.name = name;
        this.coordinates = coordinates;
        this.address = address;
        this.operatingHours = operatingHours;
        this.businessBankCenter = businessBankCenter;
        this.isClosed = isClosed;
    }

    public static BranchLocation create(String branchCode,String branchId, String name, Coordinates coordinates,
                                         BranchAddress address, OperatingHours operatingHours,
                                        boolean businessBankCenter, boolean isClosed) {
        Asserts.notBlank(branchCode, "branchCode");
        Asserts.notBlank(name, "name");
        Asserts.notNull(coordinates, "coordinates");
        Asserts.notNull(address, "address");
        Asserts.notNull(operatingHours, "operatingHours");

        return new BranchLocation(branchCode,branchId, name, coordinates, address, operatingHours, businessBankCenter, isClosed);
    }

    public static BranchLocation reconstitute(String branchCode,String branchId, String name, Coordinates coordinates,
                                               BranchAddress address, OperatingHours operatingHours
                                              ,boolean businessBankCenter, boolean isClosed) {
        return new BranchLocation(branchCode,branchId, name, coordinates, address, operatingHours, businessBankCenter, isClosed);
    }

    public double distanceFrom(Coordinates customerLocation) {
        Asserts.notNull(customerLocation, "customerLocation");
        return coordinates.distanceTo(customerLocation);
    }

    public boolean isAvailableForBooking() {
        return !isClosed;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public BranchAddress getAddress() {
        return address;
    }

    public OperatingHours getOperatingHours() {
        return operatingHours;
    }

    public boolean isBusinessBankCenter() {
        return businessBankCenter;
    }

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BranchLocation that)) return false;
        return Objects.equals(branchCode, that.branchCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(branchCode);
    }

    @Override
    public String toString() {
        return "BranchLocation{" +
                "branchCode='" + branchCode + '\'' +
                ", branchId='" + branchId + '\'' +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", address=" + address +
                ", isClosed=" + isClosed +
                '}';
    }
}

