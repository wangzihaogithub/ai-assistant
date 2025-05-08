package dev.langchain4j.data.message;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VideoArrayContent implements Content {
    private final List<VideoContent> array;

    public VideoArrayContent(VideoContent video) {
        Objects.requireNonNull(video, "VideoArrayContent video cannot be null");
        this.array = Collections.singletonList(video);
    }

    public VideoArrayContent(List<VideoContent> array) {
        this.array = Objects.requireNonNull(array, "VideoArrayContent array cannot be null");
        if (array.isEmpty()) {
            throw new IllegalArgumentException("VideoArrayContent array cannot be empty");
        }
    }

    public List<VideoContent> getArray() {
        return array;
    }

    @Override
    public ContentType type() {
        return ContentType.VIDEO;
    }
}
