package org.deeplearning4j.nn.updater.custom;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.GradientUpdater;
import org.nd4j.linalg.learning.SgdUpdater;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Sgd;

/**
 * Created by Alex on 09/05/2017.
 */
@AllArgsConstructor
@Data
public class CustomIUpdater implements IUpdater {

    public static final double DEFAULT_SGD_LR = 1e-3;

    private double learningRate;

    public CustomIUpdater() {
        this(DEFAULT_SGD_LR);
    }

    @Override
    public long stateSize(long numParams) {
        return 0;
    }

    @Override
    public void applySchedules(int iteration, double newLearningRate) {
        this.learningRate = newLearningRate;
    }

    @Override
    public GradientUpdater instantiate(INDArray viewArray, boolean initializeViewArray) {
        if (viewArray != null) {
            throw new IllegalStateException("View arrays are not supported/required for SGD updater");
        }
        return new CustomGradientUpdater(this);
    }

    @Override
    public CustomIUpdater clone() {
        return new CustomIUpdater(learningRate);
    }
}
