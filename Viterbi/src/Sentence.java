import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

class Sentence implements Iterable<Word> {
	private ArrayList<Word> sentence;

	public Sentence() {
		sentence = new ArrayList<Word>();
	}

	public void addWord(Word w) {
		sentence.add(w);
	}

	public Word getWordAt(int loc) {
		if (loc < 0 || loc > sentence.size() - 1) {
			return null;
		}
		return sentence.get(loc);
	}
	
	public void setWordPosTag(int loc, String tag) {
		Word w = sentence.get(loc);
		w.setPosTag(tag);
	}
	
	public String toString() {
		StringBuilder sent = new StringBuilder("");

		for (Word word : sentence) {
			sent.append(word.getLemme() + " ");
		}
		return sent.toString();
	}

	public int length() {
		return sentence.size();
	}

	@Override
	public Iterator<Word> iterator() {
		return sentence.iterator();
	}
}
