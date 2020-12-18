package org.tasks.activities.attribution

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.common.collect.Multimaps
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.attribution.AttributionViewModel.LibraryAttribution
import org.tasks.injection.ThemedInjectingAppCompatActivity
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class AttributionActivity : ThemedInjectingAppCompatActivity() {
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.list)
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attributions)
        ButterKnife.bind(this)
        toolbar.setTitle(R.string.third_party_licenses)
        toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationOnClickListener { finish() }
        themeColor.apply(toolbar)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        ViewModelProvider(this)
                .get(AttributionViewModel::class.java)
                .observe(this, androidx.lifecycle.Observer { libraryAttributions: List<LibraryAttribution>? ->
                    updateAttributions(libraryAttributions!!)
                })
    }

    private fun updateAttributions(libraryAttributions: List<LibraryAttribution>) {
        val rows = ArrayList<AttributionRow>()
        val byLicense = Multimaps.index(libraryAttributions) { it!!.license }
        byLicense.keySet().sorted().forEach { license ->
            rows.add(AttributionRow(license))
            rows.addAll(getRows(byLicense[license]))
        }
        recyclerView.adapter = AttributionAdapter(rows)
        Timber.d(libraryAttributions.toString())
    }

    private fun getRows(attributions: List<LibraryAttribution>): Iterable<AttributionRow> {
        val byCopyrightHolder = Multimaps.index(attributions) { lib -> lib!!.copyrightHolder }
        return byCopyrightHolder.keySet().sorted().map {
            val libraries = byCopyrightHolder[it].map { a -> "\u2022 ${a.libraryName}"}
            AttributionRow(it, libraries.sorted().joinToString("\n"))
        }
    }
}