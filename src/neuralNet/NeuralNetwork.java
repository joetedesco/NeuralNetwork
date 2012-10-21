package neuralNet;
/*
 * 	class NeuralNetwork
 *	Author: David Vincent
 *  Overview: 
 *  		This class/package is used to create an instance of an artificial neural network 
 *  	http://en.wikipedia.org/wiki/Artificial_neural_network and it uses the backpropagation
 *  	learning algorithm to train the network: http://en.wikipedia.org/wiki/Backpropagation.
 *  		To increase performance the jblas linear algebra library is used to handle the
 *  	matrix operations (http://jblas.org/). Jblas and this library have been successfully
 *  	tested on Linux (Ubuntu 12.04) with jdk: 1.7 . To use Jblas an installation of
 *  	dependencies is required (visit jblas website for more details) 
 *  		To optimize the backpropagation cost the fmincg function is used. It was written originally by
 *  	Carl Edward Rasmussen (2002), then modified by stanford university, then translated to java by thomasjungblut. 
 *  	Minor modifications were made to make the function compatible with the jblas library. 
 *  
 * 	Usage:
 * 			The instance variables of the class can be initialized by the constructors or mutator methods.
 * 		The neural network created with class can have any desired amount of layers, any desired amount of
 * 		neurons on each layer.
 * 
 * 			The itsTopology variable holds the neural network structure; for example if a nerual network
 * 		with 20 input neurons, one hidden layer with 17 neurons, and an output layer with 4 neruons was desired
 * 		the topology would be 20;17;4, so itsTopology would need to be {20,17,4}.
 * 
 * 			The connection matrices (itsTheta) should usually be generated by the static method generateThatas
 * 		but can also be loaded into the neural network manually (for example if using pre-trained weights)
 * 
 *  		The format of the training data: 
 *  			-Input training data should be a matrix where each row is a traning example and the columns are the features.
 *  			-Output training data should be a matrix where each row is a traning example and the columns are the correct outputs.
 *  
 *  		Note: the input matrix elements can be any real number (within the range of doubles). But the output matrix,
 *  	should ONLY be from the binary set {0,1}. If numbers other than the binary set of integers are used, then the network
 *  	will not train properly. 
 *  	
 *  		Example code considering the training input matrix X, and training outmatrix Y have been created.
 *  	Using a topology of 20 input neurons, two hidden layers of size 12 and 8 respectivly, and output of 9 neurons.
 *  		//-Create the neural network and initialize the weights randomly
 *  			double [] topoogy = {20,12,8,9};
 *    			NeuralNetwork testNetwork = new NeuralNetwork(topology,true);
 *    		//-Train the neural network with backprop
 *    			double lambda = 0.6987 	//used for regularization 
 *    			int iters = 500;		//number of iterations to run fmincg
 *    			boolean verbose = true  //monitor the iterations and cost of each update
 *    			testNetwork.trainBP(X,Y,lambda,iters,verbose);
 *    		//-Run the neural network and get the preidctions using same input training data
 *    			DoubleMatrix predictions = testNetwork.predictFP(X);
 *    		//-Test accuracy of neural network predictions
 *    			double accuracy = NeuralNetwork.computeAccuracy(predictions,Y);
 *  		
 */


import CostFunction;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;


public class NeuralNetwork
{

	private int [] itsTopology; 			//Neural network topology. each element inidcates neuron size on its layer.
	private Vector<DoubleMatrix> itsTheta;	//Weight matrices between each layer.


	/**
	 * Constructs empty neural network.
	 */
	public NeuralNetwork()
	{
		itsTopology=null;
		itsTheta=null;
	}
	
	/**
	 * Constructs a new neural network, with same properties of given network.
	 */
	public NeuralNetwork(NeuralNetwork nn)
	{
		this.setTopology(nn.getTopology());
		this.setTheta(nn.getTheta());
	}
	/**
	 * Constructs a new neural network, with given topology and weight matrices 
	 */
	public NeuralNetwork(int [] newTopology,Vector<DoubleMatrix> newTheta)
	{
		this.setTheta(newTheta);
		this.setTopology(newTopology);
	}
	
	/*
	 * Constructs a new neural network with given topology;
	 * 	Initializes weight matrices with random values if initWeights == true
	 */
	public NeuralNetwork(int [] newTopology, boolean initWeights)
	{
		this.setTopology(newTopology);
		if (initWeights)
			this.initWeights();
	}
	/**
	 * Initializes weight matrices with random values
	 */
	public void initWeights()
	{
		 this.setTheta(generateThetas(itsTopology));
	}
	
	/**
	 * Given an input and output matrix trains the neural network using backprop
	 */
	public void trainBP(DoubleMatrix inputs, DoubleMatrix outputs,
			double lambda, int max_iter,boolean verbose)
	{
		this.setTheta(NeuralNetwork.trainWithBackprop(inputs,outputs,
				this.getTheta(),this.getTopology(),lambda,max_iter,verbose));
		
	}
	
	/**
	 * Runs forward prop to find the hypothesis (all elements of resulting matrix are between 0 and 1 inclusively)
	 */
	public DoubleMatrix hypothesisFP(DoubleMatrix inputs)
	{
		return NeuralNetwork.forwardPropPredict(this.getTheta(), inputs);
	}
	
	/**
	 * Runs forward prop to find the prediction (all elements of resulting matrix are either 0 or 1)
	 */
	public DoubleMatrix predictFP(DoubleMatrix inputs)
	{
		DoubleMatrix hypothesis = this.hypothesisFP(inputs);
		int [] maxIndicies= hypothesis.rowArgmaxs();
		int rows = hypothesis.getRows();
		int cols = hypothesis.getColumns();
		DoubleMatrix prediction = DoubleMatrix.zeros(rows,cols);
		for (int i = 0; i< rows; i++)
		{
			prediction.put(i,maxIndicies[i],1);
		}
		return prediction;
		
	}
	
	/**
	 * Accessors and mutators:
	 */
	
	@SuppressWarnings("unchecked")
	public void setTheta(Vector<DoubleMatrix> newTheta)
	{
		itsTheta = (Vector<DoubleMatrix>) newTheta.clone();
	}
	public void setTopology(int [] newTopology)
	{
		itsTopology = newTopology;
	}

	@SuppressWarnings("unchecked")
	public Vector<DoubleMatrix> getTheta()
	{
		return (Vector<DoubleMatrix>) itsTheta.clone();
	}
	public int [] getTopology()
	{
		return itsTopology;
	}
	
	/**
	 * Static helper methods designed to aid the process of using a neural network, and for debugging.
	 */
	
	/**
	 * Prints a matrix to the standard output.
	 */
	public static void printMatrix (DoubleMatrix matrix)
	{
		double [][] matrixArray = matrix.toArray2();
		int rows = matrix.getRows();
		int cols = matrix.getColumns();
		
		for (int i = 0; i < rows; i++)
		{
			for (int j=0; j<cols; j++)
			{
				System.out.print(matrixArray[i][j] +"   ");
			}
			System.out.println();
		}
	}

	/**
	 * Prints the sizes of each matrix in the given List (or in this case Vector)
	 */
	public static void printSizes(Vector<DoubleMatrix> x)
	{
		Iterator<DoubleMatrix> iter = x.iterator();
		while (iter.hasNext())
		{
			DoubleMatrix m = iter.next();
			System.out.print(m.getRows() + ","+m.getColumns());
		}
	}
	
	/**
	 * Gets a matrix from a ascii text file.
	 * The format of the text file should have each file containing only one matrix.
	 * The format of the text file should have each row separated by a newline character
	 * The format of the text file should have each column separated by a space character (' ').
	 */
	public static DoubleMatrix getMatrixFromTextFile(String filename) throws NumberFormatException, IOException
	{
		DoubleMatrix result =null;

		String line = null;
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		Vector<Double> example = new Vector<Double>();
		Vector<double[]> examples = new Vector<double[]>();

			
		while((line = reader.readLine())!=null)
		{
			while(true)
			{
				int index =line.indexOf(' ');
				if (index>=0)
				{
					example.add(new Double(Double.parseDouble(line.substring(0,index))));
					line = line.substring(index+1,line.length());
				}
				else
				{
					break;
				}
			}
			double [] darray = new double[example.size()];
			for (int i = 0; i<example.size();i++)
			{
				darray[i]  = example.get(i).doubleValue();
			}
			examples.add(darray);
			example.removeAllElements();
			
		}
		result = new DoubleMatrix(examples.size(),examples.firstElement().length);
		for (int i = 0; i<examples.size();i++)
		{
			result.putRow(i, new DoubleMatrix(examples.get(i)));
		}
		return result;
	}
	
	/**
	 *  Takes a List (in this case Vector) and takes each element of each DoubleMatrix and places it into a column matrix.
	 *  note: the reason it is named reshapeToVector has to do with the resulting matrix, not the Java the Vector data structure
	 *  note: can be undone with reshapeToList (assuming a neural network topology exists.
	 */
	public static DoubleMatrix reshapeToVector(Vector<DoubleMatrix>inputTheta)
	{
	
		Iterator<DoubleMatrix> iter = inputTheta.iterator();
		int length =0;
		while(iter.hasNext())
		{
			length += iter.next().getLength();
		}
		iter = inputTheta.iterator();
		DoubleMatrix result = new DoubleMatrix(length,1);
		DoubleMatrix x;
		int offset=0;
		while(iter.hasNext())
		{
			x = iter.next();
			x.reshape(x.getLength(),1);
			int [] indicies = new int [x.getLength()];
			for (int i =0;i<x.getLength();i++)
			{
				indicies[i] = offset+i;
			}
			offset += x.getLength();
			result.put(indicies,0,x);
		}		
		return result;
	}

	/**
	 * Using given column matrix and given topology, takes elements from column matrix (possibly generated from reshapeToVector)
	 * and organizes them into a List (in this case Vector) of weight matrices based on given topology.
	 */
	public static Vector<DoubleMatrix> reshapeToList(DoubleMatrix x, int [] topology)
	{
		Vector<DoubleMatrix> result = new Vector<DoubleMatrix>();
		int layers = topology.length;
		
		int rows,cols;
		int offset =0;
		for (int i = 0; i<layers-1;i++)
		{
			rows = topology[i+1];
			cols = topology[i]+1;
			DoubleMatrix Theta = new DoubleMatrix(rows,cols);
			for (int j = 0;j<cols;j++)
			{
				Theta.putColumn(j,x.getRowRange(offset,offset +rows,0));
				offset+=rows;
			}
			result.add(Theta);
		}
		return result;
	}
	
	/**
	 * Helper function to compute the accuracy of predictions give said predictions and correct output matrix
	 */
	public static double computeAccuracy(DoubleMatrix predictions, DoubleMatrix Y)
	{
		return ( (predictions.mul(Y) ).sum() )*100/Y.getRows();		
	}
	
	/**
	 * Returns a matrix that has the sigmoid function applied to each element of given input matrix
	 * http://en.wikipedia.org/wiki/Sigmoid_function
	 */
	public static DoubleMatrix sigmoid(DoubleMatrix x)
	{
		DoubleMatrix result = new DoubleMatrix();
		result = x;
		result = MatrixFunctions.expi(result.mul(-1));
		result = result.add(1);
		result = MatrixFunctions.powi(result, -1);
		return result;
	}
	
	/**
	 * 		Returns a matrix that has the first derivative of the sigmoid function applied
	 *  to each element of given input matrix.
	 *  http://en.wikipedia.org/wiki/Sigmoid_function
	 */
	public static DoubleMatrix sigmoidGradiant(DoubleMatrix x)
	{
		DoubleMatrix result = new DoubleMatrix();
		result=x;
		result = sigmoid(result).mul( sigmoid(result).mul(-1).add(1) );
		return result;	
	}

	/**
	 * Returns the hypothesis of a neural network given weight matricies (Theta) and inputs (X)
	 * Uses forward propagation alrogrithm. 
	 * http://en.wikipedia.org/wiki/Feedforward_neural_network
	 */
	public static DoubleMatrix forwardPropPredict(Vector<DoubleMatrix> Theta, DoubleMatrix X)
	{
		int m = X.getRows();
		Vector<DoubleMatrix> activations = new Vector<DoubleMatrix>(Theta.size()+1);
		
		DoubleMatrix firstActivation = new DoubleMatrix(m,Theta.firstElement().getColumns() );
		firstActivation = DoubleMatrix.concatHorizontally(DoubleMatrix.ones(m,1), X);
		activations.add(firstActivation);
		
		for (int i = 1; i<Theta.size(); i++)
		{
			//matlab: a{i} = [ones(m,1) sigmoid((a{i-1})*((Theta{i-1})'))];
			//Size of a{i} is (m,c) where c is column of Theta{i-} 
			DoubleMatrix a = new DoubleMatrix(m,Theta.get(i-1).getColumns());
			a= DoubleMatrix.concatHorizontally(DoubleMatrix.ones(m,1), sigmoid(activations.get(i-1).mmul(Theta.get(i-1).transpose())));
			activations.add(a);
		}

		DoubleMatrix hypothesis = new DoubleMatrix(m,Theta.lastElement().getColumns());
		hypothesis = sigmoid(activations.lastElement().mmul(Theta.lastElement().transpose()));
		
		return hypothesis;
	}
	/**
	 * Given a neural network topology, generates connection matrices and initializes each element with random values
	 */
	public static Vector<DoubleMatrix> generateThetas(int[] topology)
	{
		int num_layers = topology.length;
		Vector<DoubleMatrix>ThetaMatrix = new Vector<DoubleMatrix>();
		for (int i=0;i<(num_layers-1);i++)
		{
			ThetaMatrix.add(DoubleMatrix.randn(topology[i+1],topology[i]+1));
		}
		return ThetaMatrix;
	}

	/**
	 * Optimizes the weight matrix using a given cost function.
	 * Obtained from https://github.com/thomasjungblut/ 
	 * A few minor changes were made to make the function compatible with jblas library.
	 */
	public static DoubleMatrix fmincg(CostFunction f,
		      DoubleMatrix pInput,  int max_iter, boolean verbose) 
	 {
		/*
		 * Minimize a continuous differentialble multivariate function. Starting point 
		 * is given by "X" (D by 1), and the function named in the string "f", must
		 * return a function value and a vector of partial derivatives. The Polack-
		 * Ribiere flavour of conjugate gradients is used to compute search directions,
		 * and a line search using quadratic and cubic polynomial approximations and the
		 * Wolfe-Powell stopping criteria is used together with the slope ratio method
		 * for guessing initial step sizes. Additionally a bunch of checks are made to
		 * make sure that exploration is taking place and that extrapolation will not
		 * be unboundedly large. The "length" gives the length of the run: if it is
		 * positive, it gives the maximum number of line searches, if negative its
		 * absolute gives the maximum allowed number of function evaluations. You can
		 * (optionally) give "length" a second component, which will indicate the
		 * reduction in function value to be expected in the first line-search (defaults
		 * to 1.0). The function returns when either its length is up, or if no further
		 * progress can be made (ie, we are at a minimum, or so close that due to
		 * numerical problems, we cannot get any closer). If the function terminates
		 * within a few iterations, it could be an indication that the function value
		 * and derivatives are not consistent (ie, there may be a bug in the
		 * implementation of your "f" function). The function returns the found
		 * solution "X", a vector of function values "fX" indicating the progress made
		 * and "i" the number of iterations (line searches or function evaluations,
		 * depending on the sign of "length") used.
		 * 
		 * Usage: [X, fX, i] = fmincg(f, X, options, P1, P2, P3, P4, P5)
		 * 
		 * See also: checkgrad 
		 * 
		 * Copyright (C) 2001 and 2002 by Carl Edward Rasmussen. Date 2002-02-13
		 * 
		 * 
		 * (C) Copyright 1999, 2000 & 2001, Carl Edward Rasmussen 
		 * Permission is granted for anyone to copy, use, or modify these
		 * programs and accompanying documents for purposes of research or
		 * education, provided this copyright notice is retained, and note is
		 * made of any changes that have been made.
		 * 
		 * These programs and documents are distributed without any warranty,
		 * express or implied. As the programs were written for research
		 * purposes only, they have not been tested to the degree that would be
		 * advisable in any important application. All use of these programs is
		 * entirely at the user's own risk.
		 * 
		 * [ml-class] Changes Made:
		 * 1) Function name and argument specifications
		 * 2) Output display
		 * 
		 * [tjungblut] Changes Made: 
		 * 1) translated from octave to java
		 * 2) added an interface to exchange minimizers more easily 
		 * BTW "fmincg" stands for Function minimize nonlinear conjugate gradient
		 * 
		 * [David Vincent] Changes Made:
		 * 1) changed matrix data structers and matrix operatons to use jblas library
		 * 2) removed (fX) column matrix that stored the cost of each iteration.
		 */
		  final double RHO = 0.01; // a bunch of constants for line
		  // searches
		  final double SIG = 0.5; // RHO and SIG are the constants in
		  // the
		  // Wolfe-Powell conditions
		  final double INT = 0.1; // don't reevaluate within 0.1 of the
		  // limit of the current bracket
		  final double EXT = 3.0; // extrapolate maximum 3 times the
		  // current bracket
		  final int MAX = 30; // max 20 function evaluations per line
		  // search
		  final int RATIO = 100; // maximum allowed slope ratio
		  DoubleMatrix input = pInput;
		  int M = 0;
		  int i = 0; // zero the run length counter
		  int red = 1; // starting point
		  int ls_failed = 0; // no previous line search has failed
		  // get function value and gradient
		  final Tuple<Double, DoubleMatrix> evaluateCost = f.evaluateCost(input);
		  double f1 = evaluateCost.getFirst();
		  DoubleMatrix df1 = evaluateCost.getSecond();
		  i = i + (max_iter < 0 ? 1 : 0);
		  DoubleMatrix s = df1.mul(-1.0d); // search direction is
		  // steepest

		  double d1 = s.mul(-1.0d).dot(s); // this is the slope
		  double z1 = red / (1.0 - d1); // initial step is red/(|s|+1)

		  while (i < Math.abs(max_iter)) 
		  {
			  i = i + (max_iter > 0 ? 1 : 0);// count iterations?!
		      // make a copy of current values
		      DoubleMatrix X0 = new DoubleMatrix().copy(input); 
		      double f0 = f1;
		      DoubleMatrix df0 = new DoubleMatrix().copy(df1);
		      // begin line search
		      input = input.add(s.mul(z1));
		      final Tuple<Double, DoubleMatrix> evaluateCost2 = f.evaluateCost(input);
		      double f2 = evaluateCost2.getFirst();
		      DoubleMatrix df2 = evaluateCost2.getSecond();

		      i = i + (max_iter < 0 ? 1 : 0); // count epochs?!
		      double d2 = df2.dot(s);
		      // initialize point 3 equal to point 1
		      double f3 = f1;
		      double d3 = d1;
		      double z3 = -z1;
		      if (max_iter > 0) {
		        M = MAX;
		      } else {
		        M = Math.min(MAX, -max_iter - i);
		      }
		      // initialize quanteties
		      int success = 0;
		      double limit = -1;

		      while (true) 
		      {
		    	  while (((f2 > f1 + z1 * RHO * d1) | (d2 > -SIG * d1)) && (M > 0)) 
		    	  {
		    		  limit = z1; // tighten the bracket
		    		  double z2 = 0.0d;
		    		  double A = 0.0d;
		    		  double B = 0.0d;
		    		  if (f2 > f1) 
		    		  {
		    			  // quadratic fit
		    			  z2 = z3 - (0.5 * d3 * z3 * z3) / (d3 * z3 + f2 - f3);
		    		  } 	
		    		  else 
		    		  {
		    			  A = 6 * (f2 - f3) / z3 + 3 * (d2 + d3); // cubic fit
		    			  B = 3 * (f3 - f2) - z3 * (d3 + 2 * d2);
		    			  // numerical error possible - ok!
		    			  z2 = (Math.sqrt(B * B - A * d2 * z3 * z3) - B) / A;
		    		  }
		    		  if (Double.isNaN(z2) || Double.isInfinite(z2)) 
		    		  {
		    			  z2 = z3 / 2.0d; // if we had a numerical problem then
		    			  // bisect
		    		  }
		    		  // don't accept too close to limits
		    		  z2 = Math.max(Math.min(z2, INT * z3), (1 - INT) * z3);
		    		  z1 = z1 + z2; // update the step
		    		  input = input.add(s.mul(z2));
		    		  final Tuple<Double, DoubleMatrix> evaluateCost3 = f
		    				  .evaluateCost(input);
		    		  f2 = evaluateCost3.getFirst();
		    		  df2 = evaluateCost3.getSecond();
		    		  M = M - 1;
		    		  i = i + (max_iter < 0 ? 1 : 0); // count epochs?!
		    		  d2 = df2.dot(s);
		    		  z3 = z3 - z2; // z3 is now relative to the location of z2
		    	  }
		    	  if (f2 > f1 + z1 * RHO * d1 || d2 > -SIG * d1) {
		    		  break; // this is a failure
		    	  }
		    	  else if (d2 > SIG * d1) 
		    	  {
		    		  success = 1;
		    		  break; // success
		    	  } else if (M == 0) 
		    	  {
		    		  break; // failure
		    	  }
		    	  double A = 6 * (f2 - f3) / z3 + 3 * (d2 + d3); // make cubic
		    	  // extrapolation
		    	  double B = 3 * (f3 - f2) - z3 * (d3 + 2 * d2);
		    	  double z2 = -d2 * z3 * z3 / (B + Math.sqrt(B * B - A * d2 * z3 * z3));
		    	  // num prob or wrong sign?
		    	  if (Double.isNaN(z2) || Double.isInfinite(z2) || z2 < 0)
		          if (limit < -0.5) 
		          { // if we have no upper limit
		        	  z2 = z1 * (EXT - 1); // the extrapolate the maximum
		        	  // amount
		          } 
		          else 
		          {
		            z2 = (limit - z1) / 2; // otherwise bisect
		          }
		    	  else if ((limit > -0.5) && (z2 + z1 > limit)) 
		    	  {
		    		  // extraplation beyond max?
		    		  z2 = (limit - z1) / 2; // bisect
		    	  }
		    	  else if ((limit < -0.5) && (z2 + z1 > z1 * EXT)) 
		    	  {
		    		  // extrapolationbeyond limit
		    		  z2 = z1 * (EXT - 1.0); // set to extrapolation limit
		    	  } 
		    	  else if (z2 < -z3 * INT) 
		    	  {
		    		  z2 = -z3 * INT;
		    	  }
		    	  else if ((limit > -0.5) && (z2 < (limit - z1) * (1.0 - INT))) 
		    	  {
		    		  // too close to the limit
		    		  z2 = (limit - z1) * (1.0 - INT);
		    	  }
		    	  // set point 3 equal to point 2
		    	  f3 = f2;
		    	  d3 = d2;
		    	  z3 = -z2;
		    	  z1 = z1 + z2;
		    	  // update current estimates
		    	  input = input.add(s.mul(z2));
		    	  final Tuple<Double, DoubleMatrix> evaluateCost3 = f.evaluateCost(input);
		    	  f2 = evaluateCost3.getFirst();
		    	  df2 = evaluateCost3.getSecond();
		    	  M = M - 1;
		    	  i = i + (max_iter < 0 ? 1 : 0); // count epochs?!
		    	  d2 = df2.dot(s);
		      }// end of line search

		      DoubleMatrix tmp = null;

		      if (success == 1) 
		      { // if line search succeeded
		    	  f1 = f2;
		    	  if (verbose)
		    		  System.out.print("Iteration " + i + " | Cost: " + f1 + "\r");
		    	  // Polack-Ribiere direction: s =
		    	  // (df2'*df2-df1'*df2)/(df1'*df1)*s - df2;
		    	  final double numerator = (df2.dot(df2) - df1.dot(df2)) / df1.dot(df1);
		    	  s = s.mul(numerator).sub(df2);
		    	  tmp = df1;
		    	  df1 = df2;
		    	  df2 = tmp; // swap derivatives
		    	  d2 = df1.dot(s);
		    	  if (d2 > 0) 
		    	  { // new slope must be negative
		    		  s = df1.mul(-1.0d); // otherwise use steepest direction
		    		  d2 = s.mul(-1.0d).dot(s);
		    	  }
		    	  // realmin in octave = 2.2251e-308
		    	  // slope ratio but max RATIO
		    	  z1 = z1 * Math.min(RATIO, d1 / (d2 - 2.2251e-308));
		    	  d1 = d2;
		    	  ls_failed = 0; // this line search did not fail
		      	} 
		      	else 
		      	{
		      		input = X0;
		      		f1 = f0;
		      		df1 = df0; // restore point from before failed line search
		      		// line search failed twice in a row?
		      		if (ls_failed == 1 || i > Math.abs(max_iter)) 
		      		{
		      			break; // or we ran out of time, so we give up
		      		}
		      		tmp = df1;
		      		df1 = df2;
		      		df2 = tmp; // swap derivatives
		      		s = df1.mul(-1.0d); // try steepest
		        	d1 = s.mul(-1.0d).dot(s);
		        	z1 = 1.0d / (1.0d - d1);
		        	ls_failed = 1; // this line search failed
		      	}
		  }

		  return input;
	 }
	/**
	 * Takes a given input, and output, and given neural network data (weight matrices and topology), and given lambda
	 * and trains the neural network using backprop for a given amount of iterations. 
	 */
	public static Vector<DoubleMatrix> trainWithBackprop(DoubleMatrix X, DoubleMatrix Y,
			Vector<DoubleMatrix> Theta,int[] topology, double lambda,int max_iter, boolean verbose)
	{
		CostFunction bpCost = new BackPropCost(X,Y,topology,lambda);
		DoubleMatrix trained_theta = fmincg(bpCost,reshapeToVector(Theta),max_iter,verbose);
		Vector<DoubleMatrix> result = reshapeToList(trained_theta,topology);
		
		return result;
	}

}