package org.urbanlaunchpad.flocktracker.models;

public class ImageKey {

  private final Integer chapterPosition;
  private final Integer questionPosition;
  private final Integer loopPosition;
  private final Integer loopIteration;

  public ImageKey(Integer chapterPosition, Integer questionPosition,
    Integer loopIteration, Integer loopPosition) {
    this.chapterPosition = chapterPosition;
    this.questionPosition = questionPosition;
    this.loopIteration = loopIteration;
    this.loopPosition = loopPosition;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof ImageKey)) {
      return false;
    }
    ImageKey key = (ImageKey) o;
    return this.chapterPosition.equals(key.chapterPosition)
           && this.questionPosition.equals(key.questionPosition);
  }

  @Override
  public String toString() {
    return "[" + chapterPosition + "," + questionPosition + ","
           + loopIteration + "," + loopPosition + "]";
  }

  public Integer getChapterPosition() {
    return chapterPosition;
  }

  public Integer getQuestionPosition() {
    return questionPosition;
  }

  public Integer getLoopPosition() {
    return loopPosition;
  }

  public Integer getLoopIteration() {
    return loopIteration;
  }
}
