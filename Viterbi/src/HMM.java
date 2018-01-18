/**
 *
 * Sachin Joshi
 * CSE 498 NLP, Fall 2017
 * Prof. Sihong Xie
 * Project 2 - Checkpoint
 * 
*/


import java.util.*;
import java.io.*;

//import Jama.Matrix;

class HMM {
	private static Map<String,Map<String, Double>> transition; // previous tag -> (current tag -> P(current|previous))
    private static Map<String,Map<String, Double>> emission; // tag -> (word -> P(tag|word))
    private static final double unseen = -100; // unseen word penalty of -100
    private static final String start = "#"; // default start

    /**
     * Train
     *
     * Method to train the POS tagger using the Hidden Markov model
     *
     * trainfile the file with the the train sentences
     * tagfile the file with the corresponding tags
     */
    private static void Train(String trainfile, String tagfile){

        BufferedReader test = null, tag = null;
        try{
            test = new BufferedReader(new FileReader(trainfile)); // Open train file
            tag = new BufferedReader(new FileReader(tagfile)); // Open tag file

            String word, tagword;

            while((word = test.readLine()) != null && (tagword = tag.readLine()) != null){ // read the file
                String[] wordarray = word.split(" "); // split by spaces
                String[] tagarray = tagword.split(" ");

                // Fill in the emission map with number of emissions
                for(int i=0; i<wordarray.length; i++){
                    if(!emission.containsKey(tagarray[i])) { // if the tag appears for the first time
                        Map<String,Double> word2value = new TreeMap<>();
                        word2value.put(wordarray[i],1.0);
                        emission.put(tagarray[i],word2value);
                    }else {
                        if (!emission.get(tagarray[i]).containsKey(wordarray[i])) { // if the word appeared with the tag for the first time
                            emission.get(tagarray[i]).put(wordarray[i], 1.0);
                        } else { // if the word has already appeared with the tag
                            double newvalue = emission.get(tagarray[i]).get(wordarray[i]) + 1;
                            emission.get(tagarray[i]).put(wordarray[i], newvalue);
                        }
                    }
                }

                // Fill in the transition map with number of transitions
                for(int i=0; i<wordarray.length; i++){
                    if(i==0){ // the first transition is # -> (first tag -> P(first tag|#)
                        if(!transition.containsKey(start)) {
                            Map<String, Double> first2value = new TreeMap<>();
                            first2value.put(tagarray[i], 1.0);
                            transition.put(start, first2value);
                        }else if(!transition.get(start).containsKey(tagarray[i])){
                            transition.get(start).put(tagarray[i], 1.0);
                        }else{
                            double newvalue = transition.get(start).get(tagarray[i]) + 1.0;
                            transition.get(start).put(tagarray[i],newvalue);
                        }

                    } else if(!transition.containsKey(tagarray[i-1])){ // if transition doesn't contain the previous tag
                        Map<String,Double> curr2value = new TreeMap<>();
                        curr2value.put(tagarray[i],1.0);
                        transition.put(tagarray[i-1],curr2value);
                    }else{ // if transition contains the previous tag
                        if(!transition.get(tagarray[i-1]).containsKey(tagarray[i])){ // but not to the current tag
                            transition.get(tagarray[i-1]).put(tagarray[i],1.0);
                        }else{ // and to the current tag
                            double newvalue = transition.get(tagarray[i-1]).get(tagarray[i]) + 1;
                            transition.get(tagarray[i-1]).put(tagarray[i],newvalue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close file if file exist. If not, catch the exception
            try {
                test.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * prepareMatrices
     *
     * Create HMM variables.
	 * Method to prepare the Matrices and normalize the probablity of the emission and transition maps and convert it to log10
     *
     */
    private static void prepareMatrices(){

        for(String a : emission.keySet()){
            double count = 0;
            for(String b : emission.get(a).keySet()) count += emission.get(a).get(b); // count the number of words in each tag
            for(String b : emission.get(a).keySet()){ // normalize the probability by dividing by count and taking log10
                double logprob = Math.log10(emission.get(a).get(b) / count);
                emission.get(a).put(b,logprob);
            }
        }

        for(String a : transition.keySet()){
            double count = 0;
            for(String b : transition.get(a).keySet()) count += transition.get(a).get(b); // count the number of transitions
            for(String b : transition.get(a).keySet()){ // normalize the probability by dividing by count and taking log10
                double logprob = Math.log10(transition.get(a).get(b) / count);
                transition.get(a).put(b,logprob);
            }
        }
    }

    /**
     * Viterbi
     *
     * Method to perform Viterbi tagging and backtracing
     *
     * Viterbi algorithm for one sentence 
	 * 
	 * input the sentence to be tagged
     * Returns result - the tags associated with the sentence
     */
    public static String Viterbi(String input){

        String result = null, lasttag = null;
        double highestscore = Double.NEGATIVE_INFINITY; // highest score in the Viterbi tagging. Set to negative infinity since score < 0
        Stack<String> toprint = new Stack<String>(); // the stack for printing to result
        List<Map<String,String>> backtrace = new ArrayList<Map<String,String>>(); // List of current tag -> previous tag
        String[] words = input.split(" "); // the array of words

        Set<String> prevstates = new TreeSet<>(); // the previous states
        prevstates.add(start); // start with "#"

        Map<String,Double> prevscores = new TreeMap<>(); // the previous scores
        prevscores.put(start,0.0); // probability of starting is 100%

        for(int i=0; i<words.length;i++){
            Set<String> nextstates = new TreeSet<>();
            Map<String,Double> nextscores = new TreeMap<>();
            Map<String,String> backpoint = new TreeMap<>();
            double score;

            for(String state: prevstates){ // for all previous tags
                if(transition.containsKey(state) && !transition.get(state).isEmpty()) {
                    for (String transit : transition.get(state).keySet()) { // for all transition of previous -> current tag
                        nextstates.add(transit);

                        if (emission.containsKey(transit) && emission.get(transit).containsKey(words[i])) { // if word is in emission
                            score = prevscores.get(state) + transition.get(state).get(transit) + emission.get(transit).get(words[i]);
                        }else { // if word is unseen, use an unseen score
                            score = prevscores.get(state) + transition.get(state).get(transit) + unseen;
                        }
                        if (!nextscores.containsKey(transit) || score > nextscores.get(transit)) {
                            nextscores.put(transit, score); // keep track of the highest score
                            backpoint.put(transit, state); // for backtracing later on
                            if(backtrace.size()>i ) backtrace.remove(i); // remove the last element if required to update the last element
                            backtrace.add(backpoint);
                        }
                    }
                }
            }
            prevscores = nextscores;
            prevstates = nextstates;
        }

        //Find the last tag for backtracing
        for(String score: prevscores.keySet()){
            if(prevscores.get(score)>highestscore){
                highestscore = prevscores.get(score);
                lasttag = score;
            }
        }

        // Perform Backtrace
        toprint.push(lasttag);
        for(int i = words.length-1; i>0; i--){
            toprint.push(backtrace.get(i).get(toprint.peek()));
        }

        //Print to result
        while(!toprint.isEmpty()){
            if(result==null) result = (toprint.pop()+" ");
            else result += (toprint.pop()+" ");
        }
        return(result);
    }

    /**
     * Prediction
     *
     * Method to perform Viterbi tagging on a file. Will return the tagged file
     * Input: filename input file to be tagged
     * Finds the most likely pos tag for each word of the sentences in the unlabeled corpus.
     */
    public static String predict(String filename){
        String tagfile = null;
        BufferedReader input = null;
        BufferedWriter output = null;
        try{
            tagfile = "data/p1/predicted_tags.txt";
            input = new BufferedReader(new FileReader(filename));
            output = new BufferedWriter(new FileWriter(tagfile));
            String line = null, tagline = null;

            while((line=input.readLine()) != null){ // perform viterbi taging line by line and write to output file
                tagline = Viterbi(line);
                output.write(tagline + "\n");
            }

        }catch(Exception e){
            e.printStackTrace();

        }finally{
            // Close file if file exist. If not, catch the exception
            try{
                input.close();
                output.close();
            }catch(Exception e){
                e.printStackTrace();
            }
            return(tagfile);
        }
    }

    /**
     * PrintStat
     *
     * Method to compare 2 tag files and print out the number of correct and incorrect tags
     * @param file1
     * @param file2
     */
    public static void PrintStat(String file1, String file2){
        BufferedReader input1 = null, input2 = null;
        try{
            input1 = new BufferedReader(new FileReader(file1));
            input2 = new BufferedReader(new FileReader(file2));
            String string1 = null, string2 = null;
            String[] stringarray1 = null, stringarray2 = null;
            int totalcount = 0, totalmistake = 0, totalcorrect = 0;

            while((string1 = input1.readLine())!=null && (string2 = input2.readLine())!=null){
                stringarray1 = string1.split(" ");
                stringarray2 = string2.split(" ");
                for(int i=0; i<stringarray1.length; i++){ // count the number of mistakes
                    if(!stringarray1[i].equals(stringarray2[i])){
                        totalmistake++;
                    }
                }
                totalcount+=stringarray1.length; // count the number of total tags
            }
            totalcorrect = totalcount - totalmistake;

            // print the statistics
            System.out.println("Result Evaluation by testing with tagging "+file1);
            System.out.println("There are "+totalcount+" tags in total.");
            System.out.println(totalcorrect+ " tags are correct and "+totalmistake+" tags are wrong.");

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                input1.close();
                input2.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

	public static void main(String[] args) throws IOException {
		/*if (args.length < 3) {
			System.out.println("Expecting at least 3 parameters");
			System.exit(0);
		}
		String labeledFileName = args[0];
		String unlabeledFileName = args[1];
		String predictionFileName = args[2];
		
		String trainingLogFileName = null;
		
		if (args.length > 3) {
			trainingLogFileName = args[3];
		}*/
		
		// read in labeled corpus
		FileHandler fh = new FileHandler();
		
		ArrayList<Sentence> labeled_corpus = fh.readTaggedSentences("data/p1/train.txt");
		
		//System.out.println(labeled_corpus.get(0));
		
		ArrayList<Sentence> unlabeled_corpus = fh.readTaggedSentences("data/p1/test.txt");

		//System.out.println(unlabeled_corpus.get(0));
		
		//System.out.println(unlabeled_corpus.size());
		//System.out.println(labeled_corpus.size());
		
		//HMM model = new HMM(labeled_corpus, unlabeled_corpus);

		//model.prepareMatrices();
		//model.em();
		//model.predict();
		//model.outputPredictions(predictionFileName);
		
		/*if (trainingLogFileName != null) {
			model.outputTrainingLog(trainingLogFileName);
		}*/
		
		/**
		 * Performing MLE
		 * To find: 1. Tansition Probabilities Matrix A
		 * 			2. Likelihood Probabilities Matrix B	
         * 
         * Performed on training labeled data train.txt
		*/
		
		// Read the training data
		BufferedReader br = new BufferedReader(new FileReader("data/p1/train.txt"));
		
		// ArrayList to store each word, each tag, word-tag pair and tag-tag pair (tag(i-1) and tag(i)) separately from the training data
		ArrayList<String> word = new ArrayList<String>(); //stores the unigram from the training data
		ArrayList<String> tag = new ArrayList<String>(); //stores the corresponding tag of the unigram 
		ArrayList<String> t1t2 = new ArrayList<String>(); //stores the tag-tag pair (bigram)
		ArrayList<String> wt = new ArrayList<String>(); //store the word and its associated tag together as a pair
		String s, s1;
		
		// Extracting the unigram and its corresponding tag from the training data and storing it in the respective ArrayLists
		while((s=br.readLine())!= null){
			ArrayList<Integer> pos = new ArrayList<Integer>();
			for(int i = 0; i < s.length(); i++){
				if((s.charAt(i)) == ' '){
					pos.add(i);
				}
			}
			if(pos.size() != 0){
				word.add(s.substring(0,pos.get(0)));
				tag.add(s.substring(pos.get(0)+1,pos.get(1)));
			}		
		}
		
		// Extracting the tag-tag pair from the training data and storing the bigram into its respective ArrayList
		BufferedWriter br1 = new BufferedWriter(new FileWriter("data/p1/t1t2.txt"));
		for(int i = 0; i < tag.size()-1; i++){
			if(((tag.get(i)).compareTo("."))==0){
			}
			else{
				br1.write(tag.get(i)+tag.get(i+1)+"\n");
				t1t2.add(tag.get(i)+tag.get(i+1));
			}
		}
		br1.close();
		//System.out.println(t1t2.get(229));	
		
		// Extracting the word-tag pair from the training data and storing the bigram into its respective ArrayList
		BufferedWriter br2 = new BufferedWriter(new FileWriter("data/p1/wt.txt"));
		for(int i = 0; i < word.size(); i++){
			wt.add(tag.get(i)+word.get(i).toLowerCase());
			br2.write(tag.get(i)+word.get(i).toLowerCase()+"\n");
		}
		br2.close();	
		
		// HashMaps to store the count of each unique unigram, each unique tag, each unique tag-tag bigram and each unique word-tag bigram
		Map<String, Integer> tCount = new HashMap<String, Integer>();
		Map<String, Integer> wCount = new HashMap<String, Integer>();
		Map<String, Integer> t1t2Count = new HashMap<String, Integer>();
		Map<String, Integer> wtCount = new HashMap<String, Integer>();
		
		// Stores the count of each unique tag
		for(String w: tag) {
			Integer count = tCount.get(w);          
			tCount.put(w, (count==null) ? 1 : count+1);
		}
		
		// Stores the count of each unique word
		for(String w: word) {
			w = w.toLowerCase();
			Integer count = wCount.get(w);          
			wCount.put(w, (count==null) ? 1 : count+1);
		}
		
		// Stores the count of each unique tag-tag bigram
		for(String w: t1t2) {
			Integer count = t1t2Count.get(w);          
			t1t2Count.put(w, (count==null) ? 1 : count+1);
		}
		
		// Stores the count of each unique word-tag bigram
		for(String w: wt) {
			Integer count = wtCount.get(w);          
			wtCount.put(w, (count==null) ? 1 : count+1);
		}	
		
		/*for(int i = 0; i < word.size(); i++){
			wcount.add(Collections.frequency(word, word.get(i)));
			tcount.add(Collections.frequency(tag, tag.get(i)));
		}*/
		
		//System.out.println(wcount.get(0));
		//System.out.println(tCount.size());
		//System.out.println(wCount.size());
		
		/** 
		 * Setting up the Matrix A to store Transition Probabilities 
		*/
		Set<String> keySetTags = tCount.keySet();
		ArrayList<String> listOfKeysTags = new ArrayList<String>(keySetTags);
		
		//System.out.println(listOfKeysTags.size());
		
		String A[][] = new String[(listOfKeysTags.size())+1][(listOfKeysTags.size())+1]; // Matrix A
		
		for(int i = 1; i < (listOfKeysTags.size())+1; i++){
			A[0][i] = listOfKeysTags.get(i-1);
			A[i][0] = listOfKeysTags.get(i-1);
		}
		
		// Calculating the Transition Probabilites 
		for(int i = 1; i < (listOfKeysTags.size())+1; i++){
			for(int j = 1; j < (listOfKeysTags.size())+1; j++){
				String tt = A[i][0]+A[0][j];
				if(t1t2Count.get(tt) == null){
					A[i][j] = "0.0";
				}
				else{
					double prob = (double)(t1t2Count.get(tt))/(tCount.get(A[i][0]));
					A[i][j] = (Double.toString(prob));
				}
			}
		}	
		
		System.out.println("Transition Pobabilities Matrix A is stored in the text file Matrix_A.txt ");
		BufferedWriter br3 = new BufferedWriter(new FileWriter("data/p1/Matrix_A.txt"));
		for(int i = 0; i < (listOfKeysTags.size())+1; i++){
			for(int j = 0; j < (listOfKeysTags.size())+1; j++){
				br3.write(A[i][j] + "\t");
			}
		br3.write("\n");
		}	
		br3.close();
		
		/** 
		 * Setting up the Matrix B to store Likelihood Probabilities 
		*/
		Set<String> keySetWords = wCount.keySet();
		ArrayList<String> listOfKeysWords = new ArrayList<String>(keySetWords);
		
		//System.out.println(listOfKeysWords);
		
		String B[][] = new String[(listOfKeysTags.size())+1][(listOfKeysWords.size())+1]; // Matrix B
		
		for(int i = 1; i < (listOfKeysTags.size())+1; i++){			
			B[i][0] = listOfKeysTags.get(i-1);
		}
		for(int i = 1; i < (listOfKeysWords.size())+1; i++){			
			B[0][i] = listOfKeysWords.get(i-1);
		}
		
		// Calculating the Likelihood Probabilities
		for(int i = 1; i < (listOfKeysTags.size())+1; i++){
			for(int j = 1; j < (listOfKeysWords.size())+1; j++){
				String tt = B[i][0]+B[0][j];
				if(wtCount.get(tt) == null){
					B[i][j] = "0.0";
				}
				else{
					double prob = (double)(wtCount.get(tt))/(tCount.get(B[i][0]));
					B[i][j] = (Double.toString(prob));
				}
			}
		}	
		
		System.out.println("Likelihood Pobabilities Matrix B is stored in the text file Matrix_B.txt ");
		BufferedWriter br4 = new BufferedWriter(new FileWriter("data/p1/Matrix_B.txt"));
		for(int i = 0; i < (listOfKeysTags.size())+1; i++){
			for(int j = 0; j < (listOfKeysWords.size())+1; j++){
				br4.write(B[i][j] + "\t");
			}
		br4.write("\n");
		}	
		br4.close();
		
		/**
		 * Read the file train.txt and test.txt
		 * Store the sentences and the corresponding tag of each unigram from the training set in train_sent.txt and trian_tags.txt respectively
		 * Store the sentences and the corresponding tag of each unigram from the testing set in test_sent.txt and test_tags.txt respectively
		 */
		BufferedWriter br5 = new BufferedWriter(new FileWriter("data/p1/train_sent.txt"));
		BufferedWriter br6 = new BufferedWriter(new FileWriter("data/p1/test_sent.txt"));
		
		BufferedReader br7 = new BufferedReader(new FileReader("data/p1/train.txt"));
		BufferedWriter br8 = new BufferedWriter(new FileWriter("data/p1/train_tags.txt"));
		
		while((s=br7.readLine())!= null){
			ArrayList<Integer> pos = new ArrayList<Integer>();
			for(int i = 0; i < s.length(); i++){
				if((s.charAt(i)) == ' '){
					pos.add(i);
				}
			}
			if(pos.size() != 0){
				br5.write((s.substring(0,pos.get(0))).toLowerCase() + " ");
				br8.write((s.substring(pos.get(0)+1,pos.get(1))) + " ");
			}
			else{
				br5.write("\n");
				br8.write("\n");
			}	
		}
		
		BufferedReader br9 = new BufferedReader(new FileReader("data/p1/test.txt"));
		BufferedWriter br10 = new BufferedWriter(new FileWriter("data/p1/test_tags.txt"));
		
		while((s=br9.readLine())!= null){
			ArrayList<Integer> pos = new ArrayList<Integer>();
			for(int i = 0; i < s.length(); i++){
				if((s.charAt(i)) == ' '){
					pos.add(i);
				}
			}
			if(pos.size() != 0){
				br6.write(((s.substring(0,pos.get(0))).toLowerCase()) + " ");
				br10.write((s.substring(pos.get(0)+1,pos.get(1))) + " ");
			}
			else{
				br6.write("\n");
				br10.write("\n");
			}	
		}
		
		br5.close();
		br6.close();
		br7.close();
		br8.close();
		br9.close();
		br10.close();
		
		// the train files
        String trainfile = "data/p1/train_sent.txt", traintag = "data/p1/train_tags.txt";
        // the test files
        String testfile = "data/p1/test_sent.txt", testtag = "data/p1/test_tags.txt";

        transition = new TreeMap<>();
        emission = new TreeMap<>();

        Train(trainfile, traintag); // train the algorithm
        prepareMatrices(); // Create HMM variables and normalize the probability

        String newtagfile = predict(testfile); // tag the file
        System.out.println("The file is tagged and saved as predictions.txt");
        PrintStat(testtag, newtagfile); // print the statistics
		
		/**
		* Output Prediction
		*
		* This section reads the tagged file predicted_tags.txt.
		* The file predicted_tags.txt is obtained from the above function predict(testfile)
		* predict(testfile); predicts the most likely POS tag for each word of the sentence in the testing data set
		* 
		* Predictions of POS tags for sentences in D are predicted and output to results/p1/predictions.txt in the desired format (unigram tokens and POS tags)
		*/
		
		// Storing the result in the file predictions.txt
		BufferedReader br11 = new BufferedReader(new FileReader("data/p1/test_sent.txt"));
		BufferedReader br12 = new BufferedReader(new FileReader("data/p1/predicted_tags.txt"));
		BufferedWriter br13 = new BufferedWriter(new FileWriter("results/p1/predictions.txt"));
		
		while((s = br11.readLine())!=null && (s1 = br12.readLine())!=null){
			String [] testword = s.split(" ");
			String [] postag = s1.split(" ");
			for(int i = 0; i < testword.length; i++){
				br13.write(testword[i] + " " + postag[i] + "\n");
			}
			br13.write("\n");
		}
		br11.close();
		br12.close();
		br13.close();		
	}
}
