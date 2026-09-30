package com.evandev.reliable_recipes.networking.legacy;

//? if <1.21 {
/*import java.util.function.BiConsumer;
import java.util.function.Function;

public interface StreamCodec<B, V> {
    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec, Function<C, T1> getter, Function<T1, C> factory) {
        return of((buf, value) -> codec.encode(buf, getter.apply(value)), buf -> factory.apply(codec.decode(buf)));
    }
}
*///?}
