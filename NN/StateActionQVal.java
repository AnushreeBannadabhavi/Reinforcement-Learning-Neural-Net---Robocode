package CPEN502.NN;

public class StateActionQVal {

    double[] StateAction;
    double Qval;

    public StateActionQVal(double[] StateActionArg, double QValArg){
        StateAction = StateActionArg;
        Qval = QValArg;
    }

    public void setStateAction(double[] stateAction) {
        StateAction = stateAction;
    }

    public double[] getStateAction() {
        return StateAction;
    }

    public double getQval() {
        return Qval;
    }

    public void setQval(double qval) {
        Qval = qval;
    }
}
