/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umcg.genetica.math.stats.concurrent;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import umcg.genetica.console.ProgressBar;
import umcg.genetica.containers.Pair;
import umcg.genetica.math.stats.Descriptives;

/**
 *
 * @author marc jan
 */
public class ConcurrentCovariation {

    private int nrThreads = Runtime.getRuntime().availableProcessors();

    public ConcurrentCovariation() {
    }

    public ConcurrentCovariation(int nrProcs) {
        nrThreads = nrProcs;
    }

    public double[][] pairwiseCovariation(double[][] in) {
        ExecutorService threadPool = Executors.newFixedThreadPool(nrThreads);
        CompletionService<Pair<Integer, double[]>> pool = new ExecutorCompletionService<Pair<Integer, double[]>>(threadPool);
        double meanOfSamples[] = new double[in.length];
        
        for(int i=0; i<meanOfSamples.length; ++i){
            meanOfSamples[i] = Descriptives.mean(in[i]);
        }
        
        for (int i = 0; i < in.length; i++) {
            ConcurrentCovariationTask task = new ConcurrentCovariationTask(in, meanOfSamples, i);
            pool.submit(task);
        }

        int returned = 0;

        double[][] covariationMatrix = new double[in.length][0];
        ProgressBar pb = new ProgressBar(in.length, "Calculation of covariation matrix: " + in.length + " x " + in.length);
        while (returned < in.length) {
            try {
                Pair<Integer, double[]> result = pool.take().get();
                if (result != null) {
                    int rownr = result.getLeft(); //  < 0 when row is not to be included because of hashProbesToInclude.
                    if (rownr >= 0) {
                        double[] doubles = result.getRight();
                        covariationMatrix[rownr] = doubles;
                    }
                    result = null;
                    returned++;
                    pb.iterate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(int r=1;r<covariationMatrix.length; r++){
            for(int c=0; c<r; c++){
                covariationMatrix[r][c] = covariationMatrix[c][r];
            }
        }
        threadPool.shutdown();
        pb.close();
        return covariationMatrix;
    }
    
    
    public DoubleMatrix2D pairwiseCovariation(DoubleMatrix2D in) {
        ExecutorService threadPool = Executors.newFixedThreadPool(nrThreads);
        CompletionService<Pair<Integer, double[]>> pool = new ExecutorCompletionService<Pair<Integer, double[]>>(threadPool);
        double meanOfSamples[] = new double[in.rows()];
        
        for(int i=0; i<in.rows(); ++i){
            meanOfSamples[i] = (in.viewRow(i).zSum())/in.columns();
        }
        
        for (int i = 0; i < in.rows(); ++i) {
            ConcurrentCovariationTask task = new ConcurrentCovariationTask(in.toArray(), meanOfSamples, i);
            pool.submit(task);
        }

        int returned = 0;

        DenseDoubleMatrix2D covariationMatrix = new DenseDoubleMatrix2D(in.rows(), in.rows());
        ProgressBar pb = new ProgressBar(in.rows(), "Calculation of covariation matrix: " + in.rows() + " x " + in.rows());
        while (returned < in.rows()) {
            try {
                Pair<Integer, double[]> result = pool.take().get();
                if (result != null) {
                    int rownr = result.getLeft(); //  < 0 when row is not to be included because of hashProbesToInclude.
                    if (rownr >= 0) {
                        double[] doubles = result.getRight();
                        for(int i=0; i< in.rows(); i++){
                            covariationMatrix.setQuick(rownr, i, doubles[i]);
                        }
                    }
                    result = null;
                    returned++;
                    pb.iterate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(int r=1;r<covariationMatrix.rows(); r++){
            for(int c=0; c<r; c++){
                covariationMatrix.setQuick(r, c, covariationMatrix.getQuick(c, r));
            }
        }
        threadPool.shutdown();
        pb.close();
        return covariationMatrix;
    }
}
