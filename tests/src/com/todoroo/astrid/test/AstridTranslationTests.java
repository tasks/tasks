package com.todoroo.astrid.test;



import com.timsu.astrid.R;
import com.todoroo.andlib.test.TranslationTests;

public class AstridTranslationTests extends TranslationTests {

    @Override
    public Class<?> getArrayResources() {
        return R.array.class;
    }

    @Override
    public Class<?> getStringResources() {
        return R.string.class;
    }

    @Override
    public int[] getDateFormatStrings() {
        return new int[] {
                //
        };
    }

}
