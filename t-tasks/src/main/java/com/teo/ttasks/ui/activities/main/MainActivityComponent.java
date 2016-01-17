package com.teo.ttasks.ui.activities.main;

import com.teo.ttasks.injection.ActivityScope;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    MainActivity inject(MainActivity mainActivity);

}
