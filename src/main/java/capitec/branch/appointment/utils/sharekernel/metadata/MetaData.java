package capitec.branch.appointment.utils.sharekernel.metadata;

import java.io.Serializable;

public sealed interface MetaData extends Serializable permits AppointmentMetadata, OTPMetadata  {
}
