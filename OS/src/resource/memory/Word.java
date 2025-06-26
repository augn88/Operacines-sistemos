package resource.memory;

import static util.RegisterUtils.toHexWithPadding;

public class Word {
  public static final int CHARS_PER_WORD = 4;

  private String data = "0000";


  public Word(int s) {
    setIntValue(s);
  }

  public Word(String s) {
    setWordData(s);
  }

  public static Word[] convertFromString(String string) {
    Word[] words = new Word[string.length() / CHARS_PER_WORD];

    for (int i = 0, wordStart = 0; wordStart < string.length(); i++, wordStart += CHARS_PER_WORD) {
      words[i] = new Word(string.substring(wordStart, wordStart + CHARS_PER_WORD));
    }

    return words;
  }

  public void setIntValue(int integer) {
    setWordData(toHexWithPadding(integer, CHARS_PER_WORD));
  }

  public String getWordData() {
    return data.toUpperCase();
  }

  public void setWordData(String s) {
    if (s.length() <= CHARS_PER_WORD) {
      while (s.length() < CHARS_PER_WORD) {
        s = s + " ";
      }
      data = s;
    }
    else {
      data = s.substring(0, CHARS_PER_WORD);
    }
  }

  @Override
  public String toString() {
    return data;
  }
}