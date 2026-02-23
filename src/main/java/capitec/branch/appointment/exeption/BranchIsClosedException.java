package capitec.branch.appointment.exeption;

public class BranchIsClosedException extends  RuntimeException  {
    public BranchIsClosedException(String s) {
        super(s);
    }
}
