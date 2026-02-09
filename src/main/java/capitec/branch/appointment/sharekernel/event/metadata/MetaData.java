package capitec.branch.appointment.sharekernel.event.metadata;

import java.io.Serializable;

public sealed interface MetaData extends Serializable permits AppointmentMetadata, OTPMetadata  {
}
