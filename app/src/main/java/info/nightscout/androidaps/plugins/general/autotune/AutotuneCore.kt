package info.nightscout.androidaps.plugins.general.autotune

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.R
//import info.nightscout.androidaps.interfaces.ActivePluginProvider
import info.nightscout.androidaps.plugins.general.autotune.data.ATProfile
import info.nightscout.androidaps.plugins.general.autotune.data.PreppedGlucose
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin
import info.nightscout.androidaps.utils.Round
import info.nightscout.shared.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutotuneCore(private val injector: HasAndroidInjector) {
    //@Inject lateinit var activePlugin: ActivePluginProvider
    @Inject lateinit var  sp: SP
    @Inject lateinit var  autotunePlugin: AutotunePlugin

    fun tuneAllTheThings(preppedGlucose: PreppedGlucose, previousAutotune: ATProfile, pumpProfile: ATProfile): ATProfile {
        //var pumpBasalProfile = pumpProfile.basalprofile;
        val pumpBasalProfile = pumpProfile.basal
        //console.error(pumpBasalProfile);
        var basalProfile = previousAutotune.basal
        //console.error(basalProfile);
        //console.error(isfProfile);
        var isf = previousAutotune.isf
        //console.error(isf);
        var carbRatio = previousAutotune.ic
        //console.error(carbRatio);
        var csf = isf / carbRatio
        //val dia = previousAutotune.dia
        //val insulinInterface = activePlugin.activeInsulin
        //var peak = 75
        //if (insulinInterface.id == InsulinInterface.InsulinType.OREF_ULTRA_RAPID_ACTING) peak = 55 else if (insulinInterface.id == InsulinInterface.InsulinType.OREF_FREE_PEAK) peak = sp.getInt(R.string.key_insulin_oref_peak, 75)
        val csfGlucose = preppedGlucose.csfGlucoseData
        val isfGlucose = preppedGlucose.isfGlucoseData
        val basalGlucose = preppedGlucose.basalGlucoseData
        val crData = preppedGlucose.crData
        //List<DiaDatum> diaDeviations = preppedGlucose.diaDeviations;
        //List<PeakDatum> peakDeviations = preppedGlucose.peakDeviations;
        val pumpISF = pumpProfile.isf
        val pumpCarbRatio = pumpProfile.ic
        val pumpCSF = pumpISF / pumpCarbRatio
        // Autosens constraints
        val autotuneMax = sp.getDouble(R.string.key_openapsama_autosens_max, 1.2)
        val autotuneMin = sp.getDouble(R.string.key_openapsama_autosens_min, 0.7)
        val min5minCarbImpact = sp.getDouble(R.string.key_openapsama_min_5m_carbimpact, 3.0)

        /*******Tune DIA (#57-#99) and Peak (#101-#139) disabled for the first version code below in js********************************************************************************************************
         * // tune DIA
         * var newDIA = DIA;
         * if (diaDeviations) {
         * var currentDIAMeanDev = diaDeviations[2].meanDeviation;
         * var currentDIARMSDev = diaDeviations[2].RMSDeviation;
         * //console.error(DIA,currentDIAMeanDev,currentDIARMSDev);
         * var minMeanDeviations = 1000000;
         * var minRMSDeviations = 1000000;
         * var meanBest = 2;
         * var RMSBest = 2;
         * for (var i=0; i < diaDeviations.length; i++) {
         * var meanDeviations = diaDeviations[i].meanDeviation;
         * var RMSDeviations = diaDeviations[i].RMSDeviation;
         * if (meanDeviations < minMeanDeviations) {
         * minMeanDeviations = Math.round(meanDeviations*1000)/1000;
         * meanBest = i;
         * }
         * if (RMSDeviations < minRMSDeviations) {
         * minRMSDeviations = Math.round(RMSDeviations*1000)/1000;
         * RMSBest = i;
         * }
         * }
         * console.error("Best insulinEndTime for meanDeviations:",diaDeviations[meanBest].dia,"hours");
         * console.error("Best insulinEndTime for RMSDeviations:",diaDeviations[RMSBest].dia,"hours");
         * if ( meanBest < 2 && RMSBest < 2 ) {
         * if ( diaDeviations[1].meanDeviation < currentDIAMeanDev * 0.99 && diaDeviations[1].RMSDeviation < currentDIARMSDev * 0.99 ) {
         * newDIA = diaDeviations[1].dia;
         * }
         * } else if ( meanBest > 2 && RMSBest > 2 ) {
         * if ( diaDeviations[3].meanDeviation < currentDIAMeanDev * 0.99 && diaDeviations[3].RMSDeviation < currentDIARMSDev * 0.99 ) {
         * newDIA = diaDeviations[3].dia;
         * }
         * }
         * if ( newDIA > 12 ) {
         * console.error("insulinEndTime maximum is 12h: not raising further");
         * newDIA=12;
         * }
         * if ( newDIA !== DIA ) {
         * console.error("Adjusting insulinEndTime from",DIA,"to",newDIA,"hours");
         * } else {
         * console.error("Leaving insulinEndTime unchanged at",DIA,"hours");
         * }
         * }
         *
         * // tune insulinPeakTime
         * var newPeak = peak;
         * if (peakDeviations && peakDeviations[2]) {
         * var currentPeakMeanDev = peakDeviations[2].meanDeviation;
         * var currentPeakRMSDev = peakDeviations[2].RMSDeviation;
         * //console.error(currentPeakMeanDev);
         * minMeanDeviations = 1000000;
         * minRMSDeviations = 1000000;
         * meanBest = 2;
         * RMSBest = 2;
         * for (i=0; i < peakDeviations.length; i++) {
         * meanDeviations = peakDeviations[i].meanDeviation;
         * RMSDeviations = peakDeviations[i].RMSDeviation;
         * if (meanDeviations < minMeanDeviations) {
         * minMeanDeviations = Math.round(meanDeviations*1000)/1000;
         * meanBest = i;
         * }
         * if (RMSDeviations < minRMSDeviations) {
         * minRMSDeviations = Math.round(RMSDeviations*1000)/1000;
         * RMSBest = i;
         * }
         * }
         * console.error("Best insulinPeakTime for meanDeviations:",peakDeviations[meanBest].peak,"minutes");
         * console.error("Best insulinPeakTime for RMSDeviations:",peakDeviations[RMSBest].peak,"minutes");
         * if ( meanBest < 2 && RMSBest < 2 ) {
         * if ( peakDeviations[1].meanDeviation < currentPeakMeanDev * 0.99 && peakDeviations[1].RMSDeviation < currentPeakRMSDev * 0.99 ) {
         * newPeak = peakDeviations[1].peak;
         * }
         * } else if ( meanBest > 2 && RMSBest > 2 ) {
         * if ( peakDeviations[3].meanDeviation < currentPeakMeanDev * 0.99 && peakDeviations[3].RMSDeviation < currentPeakRMSDev * 0.99 ) {
         * newPeak = peakDeviations[3].peak;
         * }
         * }
         * if ( newPeak !== peak ) {
         * console.error("Adjusting insulinPeakTime from",peak,"to",newPeak,"minutes");
         * } else {
         * console.error("Leaving insulinPeakTime unchanged at",peak);
         * }
         * }
         *
         */

        // Calculate carb ratio (CR) independently of csf and isf
        // Use the time period from meal bolus/carbs until COB is zero and IOB is < currentBasal/2
        // For now, if another meal IOB/COB stacks on top of it, consider them together
        // Compare beginning and ending BGs, and calculate how much more/less insulin is needed to neutralize
        // Use entered carbs vs. starting IOB + delivered insulin + needed-at-end insulin to directly calculate CR.

        //autotune-core (lib/autotune/index.js) #149-#165
        var crTotalCarbs = 0.0
        var crTotalInsulin = 0.0
        for (i in crData!!.indices) {
            val crDatum = crData[i]
            val crBGChange = crDatum.crEndBG - crDatum.crInitialBG
            val crInsulinReq = crBGChange / isf
            //val crIOBChange = crDatum.crEndIOB - crDatum.crInitialIOB
            crDatum.crInsulinTotal = crDatum.crInitialIOB + crDatum.crInsulin + crInsulinReq
            //log(crDatum.crInitialIOB + " " + crDatum.crInsulin + " " + crInsulinReq + " " + crDatum.crInsulinTotal);
            //val cr = Round.roundTo(crDatum.crCarbs / crDatum.crInsulinTotal, 0.001)
            //log(crBGChange + " " + crInsulinReq + " " + crIOBChange + " " + crDatum.crInsulinTotal);
            //log("CRCarbs: " + crDatum.crCarbs + " CRInsulin: " + crDatum.crInsulinTotal + " CR:" + cr);
            if (crDatum.crInsulinTotal > 0) {
                crTotalCarbs += crDatum.crCarbs
                crTotalInsulin += crDatum.crInsulinTotal
            }
        }

        //autotune-core (lib/autotune/index.js) #166-#169
        crTotalInsulin = Round.roundTo(crTotalInsulin, 0.001)
        var totalCR = 0.0
        if (crTotalInsulin != 0.0)
            totalCR = Round.roundTo(crTotalCarbs / crTotalInsulin, 0.001)
        log("crTotalCarbs: $crTotalCarbs crTotalInsulin: $crTotalInsulin totalCR: $totalCR")

        //autotune-core (lib/autotune/index.js) #170-#209 (already hourly in aaps)
        // convert the basal profile to hourly if it isn't already
        val hourlyBasalProfile = basalProfile

        //log(hourlyPumpProfile.toString());
        //log(hourlyBasalProfile.toString());
        val newHourlyBasalProfile = DoubleArray(24)
        for (i in 0..23) {
            newHourlyBasalProfile[i] = hourlyBasalProfile[i]
        }
        val basalUntuned = previousAutotune.basalUntuned

        //autotune-core (lib/autotune/index.js) #210-#266
        // look at net deviations for each hour
        for (hour in 0..23) {
            var deviations = 0.0
            for (i in basalGlucose!!.indices) {
                val BGTime = Calendar.getInstance()
                //var BGTime: Date? = null
                if (basalGlucose[i].date != 0L) {
                    BGTime.setTimeInMillis(basalGlucose[i].date)
                    //BGTime = Date(basalGlucose[i].date)
                } else {
                    log("Could not determine last BG time")
                }
                val myHour = BGTime.get(Calendar.HOUR_OF_DAY)
                //val myHour = BGTime!!.hours
                if (hour == myHour) {
                    //log.debug(basalGlucose[i].deviation);
                    deviations += basalGlucose[i].deviation
                }
            }
            deviations = Round.roundTo(deviations, 0.001)
            log("Hour $hour total deviations: $deviations mg/dL")
            // calculate how much less or additional basal insulin would have been required to eliminate the deviations
            // only apply 20% of the needed adjustment to keep things relatively stable
            var basalNeeded = 0.2 * deviations / isf
            basalNeeded = Round.roundTo(basalNeeded, 0.01)
            // if basalNeeded is positive, adjust each of the 1-3 hour prior basals by 10% of the needed adjustment
            log("Hour $hour basal adjustment needed: $basalNeeded U/hr")
            if (basalNeeded > 0) {
                for (offset in -3..-1) {
                    var offsetHour = hour + offset
                    if (offsetHour < 0) {
                        offsetHour += 24
                    }
                    //log.debug(offsetHour);
                    newHourlyBasalProfile[offsetHour] = newHourlyBasalProfile[offsetHour] + basalNeeded / 3
                    newHourlyBasalProfile[offsetHour] = Round.roundTo(newHourlyBasalProfile[offsetHour], 0.001)
                }
                // otherwise, figure out the percentage reduction required to the 1-3 hour prior basals
                // and adjust all of them downward proportionally
            } else if (basalNeeded < 0) {
                var threeHourBasal = 0.0
                for (offset in -3..-1) {
                    var offsetHour = hour + offset
                    if (offsetHour < 0) {
                        offsetHour += 24
                    }
                    threeHourBasal += newHourlyBasalProfile[offsetHour]
                }
                val adjustmentRatio = 1.0 + basalNeeded / threeHourBasal
                //log.debug(adjustmentRatio);
                for (offset in -3..-1) {
                    var offsetHour = hour + offset
                    if (offsetHour < 0) {
                        offsetHour += 24
                    }
                    newHourlyBasalProfile[offsetHour] = newHourlyBasalProfile[offsetHour] * adjustmentRatio
                    newHourlyBasalProfile[offsetHour] = Round.roundTo(newHourlyBasalProfile[offsetHour], 0.001)
                }
            }
        }
        //autotune-core (lib/autotune/index.js) #267-#294
        for (hour in 0..23) {
            //log.debug(newHourlyBasalProfile[hour],hourlyPumpProfile[hour].rate*1.2);
            // cap adjustments at autosens_max and autosens_min
            val maxRate = pumpBasalProfile[hour] * autotuneMax
            val minRate = pumpBasalProfile[hour] * autotuneMin
            if (newHourlyBasalProfile[hour] > maxRate) {
                log("Limiting hour " + hour + " basal to " + Round.roundTo(maxRate, 0.01) + " (which is " + Round.roundTo(autotuneMax, 0.01) + " * pump basal of " + pumpBasalProfile[hour] + ")")
                //log.debug("Limiting hour",hour,"basal to",maxRate.toFixed(2),"(which is 20% above pump basal of",hourlyPumpProfile[hour].rate,")");
                newHourlyBasalProfile[hour] = maxRate
            } else if (newHourlyBasalProfile[hour] < minRate) {
                log("Limiting hour " + hour + " basal to " + Round.roundTo(minRate, 0.01) + " (which is " + autotuneMin + " * pump basal of " + newHourlyBasalProfile[hour] + ")")
                //log.debug("Limiting hour",hour,"basal to",minRate.toFixed(2),"(which is 20% below pump basal of",hourlyPumpProfile[hour].rate,")");
                newHourlyBasalProfile[hour] = minRate
            }
            newHourlyBasalProfile[hour] = Round.roundTo(newHourlyBasalProfile[hour], 0.001)
        }

        // some hours of the day rarely have data to tune basals due to meals.
        // when no adjustments are needed to a particular hour, we should adjust it toward the average of the
        // periods before and after it that do have data to be tuned
        var lastAdjustedHour = 0
        // scan through newHourlyBasalProfile and find hours where the rate is unchanged
        //autotune-core (lib/autotune/index.js) #302-#323
        for (hour in 0..23) {
            if (hourlyBasalProfile[hour] == newHourlyBasalProfile[hour]) {
                var nextAdjustedHour = 23
                for (nextHour in hour..23) {
                    if (hourlyBasalProfile[nextHour] != newHourlyBasalProfile[nextHour]) {
                        nextAdjustedHour = nextHour
                        break
                        //} else {
                        //    log("At hour: "+nextHour +" " + hourlyBasalProfile[nextHour] + " " +newHourlyBasalProfile[nextHour]);
                    }
                }
                //log.debug(hour, newHourlyBasalProfile);
                newHourlyBasalProfile[hour] = Round.roundTo(0.8 * hourlyBasalProfile[hour] + 0.1 * newHourlyBasalProfile[lastAdjustedHour] + 0.1 * newHourlyBasalProfile[nextAdjustedHour], 0.001)
                basalUntuned[hour]++
                log("Adjusting hour " + hour + " basal from " + hourlyBasalProfile[hour] + " to " + newHourlyBasalProfile[hour] + " based on hour " + lastAdjustedHour + " = " + newHourlyBasalProfile[lastAdjustedHour] + " and hour " + nextAdjustedHour + " = " + newHourlyBasalProfile[nextAdjustedHour])
            } else {
                lastAdjustedHour = hour
            }
        }
        //log(newHourlyBasalProfile.toString());
        basalProfile = newHourlyBasalProfile

        // Calculate carb ratio (CR) independently of csf and isf
        // Use the time period from meal bolus/carbs until COB is zero and IOB is < currentBasal/2
        // For now, if another meal IOB/COB stacks on top of it, consider them together
        // Compare beginning and ending BGs, and calculate how much more/less insulin is needed to neutralize
        // Use entered carbs vs. starting IOB + delivered insulin + needed-at-end insulin to directly calculate CR.

        // calculate net deviations while carbs are absorbing
        // measured from carb entry until COB and deviations both drop to zero
        var deviations = 0.0
        var mealCarbs = 0
        var totalMealCarbs = 0
        var totalDeviations = 0.0
        val fullNewCSF: Double
        //log.debug(CSFGlucose[0].mealAbsorption);
        //log.debug(CSFGlucose[0]);
        //autotune-core (lib/autotune/index.js) #346-#365
        for (i in csfGlucose!!.indices) {
            //log.debug(CSFGlucose[i].mealAbsorption, i);
            if (csfGlucose[i].mealAbsorption === "start") {
                deviations = 0.0
                mealCarbs = csfGlucose[i].mealCarbs
            } else if (csfGlucose[i].mealAbsorption === "end") {
                deviations += csfGlucose[i].deviation
                // compare the sum of deviations from start to end vs. current csf * mealCarbs
                //log.debug(csf,mealCarbs);
                //val csfRise = csf * mealCarbs
                //log.debug(deviations,isf);
                //log.debug("csfRise:",csfRise,"deviations:",deviations);
                totalMealCarbs += mealCarbs
                totalDeviations += deviations
            } else {
                //todo Philoul check 0 * min5minCarbImpact ???
                deviations += Math.max(0 * min5minCarbImpact, csfGlucose[i].deviation)
                mealCarbs = Math.max(mealCarbs, csfGlucose[i].mealCarbs)
            }
        }
        // at midnight, write down the mealcarbs as total meal carbs (to prevent special case of when only one meal and it not finishing absorbing by midnight)
        // TODO: figure out what to do with dinner carbs that don't finish absorbing by midnight
        if (totalMealCarbs == 0) {
            totalMealCarbs += mealCarbs
        }
        if (totalDeviations == 0.0) {
            totalDeviations += deviations
        }
        //log.debug(totalDeviations, totalMealCarbs);
        fullNewCSF = if (totalMealCarbs == 0) {
            // if no meals today, csf is unchanged
            csf
        } else {
            // how much change would be required to account for all of the deviations
            Round.roundTo(totalDeviations / totalMealCarbs, 0.01)
        }
        // only adjust by 20%
        var newCSF = 0.8 * csf + 0.2 * fullNewCSF
        // safety cap csf
        if (pumpCSF != 0.0) {
            val maxCSF = pumpCSF * autotuneMax
            val minCSF = pumpCSF * autotuneMin
            if (newCSF > maxCSF) {
                log("Limiting csf to " + Round.roundTo(maxCSF, 0.01) + " (which is " + autotuneMax + "* pump csf of " + pumpCSF + ")")
                newCSF = maxCSF
            } else if (newCSF < minCSF) {
                log("Limiting csf to " + Round.roundTo(minCSF, 0.01) + " (which is" + autotuneMin + "* pump csf of " + pumpCSF + ")")
                newCSF = minCSF
            } //else { log.debug("newCSF",newCSF,"is close enough to",pumpCSF); }
        }
        val oldCSF = Round.roundTo(csf, 0.001)
        newCSF = Round.roundTo(newCSF, 0.001)
        totalDeviations = Round.roundTo(totalDeviations, 0.001)
        log("totalMealCarbs: $totalMealCarbs totalDeviations: $totalDeviations oldCSF $oldCSF fullNewCSF: $fullNewCSF newCSF: $newCSF")
        // this is where csf is set based on the outputs
        //if (newCSF != 0.0) {
        //    csf = newCSF
        //}
        var fullNewCR: Double
        fullNewCR = if (totalCR == 0.0) {
            // if no meals today, CR is unchanged
            carbRatio
        } else {
            // how much change would be required to account for all of the deviations
            totalCR
        }
        // don't tune CR out of bounds
        var maxCR = pumpCarbRatio * autotuneMax
        if (maxCR > 150) {
            maxCR = 150.0
        }
        var minCR = pumpCarbRatio * autotuneMin
        if (minCR < 3) {
            minCR = 3.0
        }
        // safety cap fullNewCR
        if (pumpCarbRatio != 0.0) {
            if (fullNewCR > maxCR) {
                log("Limiting fullNewCR from " + fullNewCR + " to " + Round.roundTo(maxCR, 0.01) + " (which is " + autotuneMax + " * pump CR of " + pumpCarbRatio + ")")
                fullNewCR = maxCR
            } else if (fullNewCR < minCR) {
                log("Limiting fullNewCR from " + fullNewCR + " to " + Round.roundTo(minCR, 0.01) + " (which is " + autotuneMin + " * pump CR of " + pumpCarbRatio + ")")
                fullNewCR = minCR
            } //else { log.debug("newCR",newCR,"is close enough to",pumpCarbRatio); }
        }
        // only adjust by 20%
        var newCR = 0.8 * carbRatio + 0.2 * fullNewCR
        // safety cap newCR
        if (pumpCarbRatio != 0.0) {
            if (newCR > maxCR) {
                log("Limiting CR to " + Round.roundTo(maxCR, 0.01) + " (which is " + autotuneMax + " * pump CR of " + pumpCarbRatio + ")")
                newCR = maxCR
            } else if (newCR < minCR) {
                log("Limiting CR to " + Round.roundTo(minCR, 0.01) + " (which is " + autotuneMin + " * pump CR of " + pumpCarbRatio + ")")
                newCR = minCR
            } //else { log.debug("newCR",newCR,"is close enough to",pumpCarbRatio); }
        }
        newCR = Round.roundTo(newCR, 0.001)
        log("oldCR: $carbRatio fullNewCR: $fullNewCR newCR: $newCR")
        // this is where CR is set based on the outputs
        //var ISFFromCRAndCSF = isf;
        if (newCR != 0.0) {
            carbRatio = newCR
            //ISFFromCRAndCSF = Math.round( carbRatio * csf * 1000)/1000;
        }

        // calculate median deviation and bgi in data attributable to isf
        val isfDeviations: MutableList<Double> = ArrayList()
        val bGIs: MutableList<Double> = ArrayList()
        val avgDeltas: MutableList<Double> = ArrayList()
        val ratios: MutableList<Double> = ArrayList()
        var count = 0
        for (i in isfGlucose!!.indices) {
            val deviation = isfGlucose[i].deviation
            isfDeviations.add(deviation)
            val BGI = isfGlucose[i].bgi
            bGIs.add(BGI)
            val avgDelta = isfGlucose[i].avgDelta
            avgDeltas.add(avgDelta)
            val ratio = 1 + deviation / BGI
            //log.debug("Deviation:",deviation,"BGI:",BGI,"avgDelta:",avgDelta,"ratio:",ratio);
            ratios.add(ratio)
            count++
        }
        Collections.sort(avgDeltas)
        Collections.sort(bGIs)
        Collections.sort(isfDeviations)
        Collections.sort(ratios)
        var p50deviation = IobCobCalculatorPlugin.percentile(isfDeviations.toTypedArray(), 0.50)
        var p50BGI = IobCobCalculatorPlugin.percentile(bGIs.toTypedArray(), 0.50)
        val p50ratios = Round.roundTo(IobCobCalculatorPlugin.percentile(ratios.toTypedArray(), 0.50), 0.001)
        var fullNewISF = isf
        if (count < 10) {
            // leave isf unchanged if fewer than 5 isf data points
            log("Only found " + isfGlucose.size + " ISF data points, leaving ISF unchanged at " + isf)
        } else {
            // calculate what adjustments to isf would have been necessary to bring median deviation to zero
            fullNewISF = isf * p50ratios
        }
        fullNewISF = Round.roundTo(fullNewISF, 0.001)
        // adjust the target isf to be a weighted average of fullNewISF and pumpISF
        val adjustmentFraction: Double
        /*
        // TODO: philoul may be allow adjustmentFraction in settings with safety limits ?)
        if (typeof(pumpProfile.autotune_isf_adjustmentFraction) !== 'undefined') {
            adjustmentFraction = pumpProfile.autotune_isf_adjustmentFraction;
        } else {*/
        adjustmentFraction = 1.0
        //        }

        // low autosens ratio = high isf
        val maxISF = pumpISF / autotuneMin
        // high autosens ratio = low isf
        val minISF = pumpISF / autotuneMax
        var adjustedISF = 0.0
        var newISF = 0.0
        if (pumpISF != 0.0) {
            adjustedISF = if (fullNewISF < 0) {
                isf
            } else {
                adjustmentFraction * fullNewISF + (1 - adjustmentFraction) * pumpISF
            }
            // cap adjustedISF before applying 10%
            //log.debug(adjustedISF, maxISF, minISF);
            if (adjustedISF > maxISF) {
                log("Limiting adjusted isf of " + Round.roundTo(adjustedISF, 0.01) + " to " + Round.roundTo(maxISF, 0.01) + "(which is pump isf of " + pumpISF + "/" + autotuneMin + ")")
                adjustedISF = maxISF
            } else if (adjustedISF < minISF) {
                log("Limiting adjusted isf of" + Round.roundTo(adjustedISF, 0.01) + " to " + Round.roundTo(minISF, 0.01) + "(which is pump isf of " + pumpISF + "/" + autotuneMax + ")")
                adjustedISF = minISF
            }

            // and apply 20% of that adjustment
            newISF = 0.8 * isf + 0.2 * adjustedISF
            if (newISF > maxISF) {
                log("Limiting isf of" + Round.roundTo(newISF, 0.01) + "to" + Round.roundTo(maxISF, 0.01) + "(which is pump isf of" + pumpISF + "/" + autotuneMin + ")")
                newISF = maxISF
            } else if (newISF < minISF) {
                log("Limiting isf of" + Round.roundTo(newISF, 0.01) + "to" + Round.roundTo(minISF, 0.01) + "(which is pump isf of" + pumpISF + "/" + autotuneMax + ")")
                newISF = minISF
            }
        }
        newISF = Round.roundTo(newISF, 0.001)
        //log.debug(avgRatio);
        //log.debug(newISF);
        p50deviation = Round.roundTo(p50deviation, 0.001)
        p50BGI = Round.roundTo(p50BGI, 0.001)
        adjustedISF = Round.roundTo(adjustedISF, 0.001)
        log("p50deviation: $p50deviation p50BGI $p50BGI p50ratios: $p50ratios Old isf: $isf fullNewISF: $fullNewISF adjustedISF: $adjustedISF newISF: $newISF")
        if (newISF != 0.0) {
            isf = newISF
        }
        previousAutotune.from = preppedGlucose.from
        previousAutotune.basal = basalProfile
        previousAutotune.isf = isf
        previousAutotune.ic = Round.roundTo(carbRatio, 0.001)
        previousAutotune.basalUntuned = basalUntuned
        /* code prepared for future dia/peak integration
        previousAutotune.dia=newDia;
        previousAutotune.peak = newPeak ;
        if (diaDeviations || peakDeviations) {
            autotuneOutput.useCustomPeakTime = true;
        }
        */
        previousAutotune.updateProfile()
        return previousAutotune
    }

    private fun log(message: String) {
        autotunePlugin.atLog("[Core] $message")
    }

    init {
        injector.androidInjector().inject(this)
    }
}