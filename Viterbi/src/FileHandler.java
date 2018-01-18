/**
 *
 * This class reads in labeled (tagged) data, unlabeled data.
 * It can also output tagged data.
  */

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.AbstractMap.*;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;

class FileHandler {
	/*
	   Read tagged sentences and return a list of tagged sentences.
	*/
	ArrayList<Sentence> readTaggedSentences(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));

		String cur_line = null;
	
		ArrayList<Sentence> taggedSentences = new ArrayList<Sentence>();

		int num_sentences = 0;
		boolean is_first = true;

		Sentence sent = null;

		while ((cur_line = br.readLine()) != null) {
			if (is_first || cur_line.length() == 0) {
				if (is_first) {
					is_first = false;
					num_sentences++;
					sent = new Sentence();
					String[] parts = cur_line.split(" ");
					sent.addWord(new Word(parts[0], parts[1]));

				} else {
					// add the finished sentence
					taggedSentences.add(sent);
					
					// create new empty sentence
					num_sentences++;
					sent = new Sentence();
				}
			} else {
				/*
				if (sent == null) {
					System.out.println("null sent pointer");
				}
				*/
				String[] parts = cur_line.split(" ");
				Word w = new Word(parts[0], parts[1]);
				/*
				if (w == null) {
					System.out.println("null word pointer");
				}
				*/
				sent.addWord(w);
			}
		}

		return taggedSentences;
	}

	public static void main(String[] args) throws IOException {
		String trainFilename = args[0];

		FileHandler fh = new FileHandler();

		ArrayList<Sentence> corpus = fh.readTaggedSentences(trainFilename);

		//System.out.println(corpus.get(0).toString());
		//System.out.println(corpus.get(1).toString());
		//System.out.println(corpus.get(corpus.size() - 1).toString());
		System.out.println(corpus.size());
		//System.out.println(corpus);
	}
}
