# ATMOS data contract

ATMOS treats weather data as evidence, not decoration. This document defines exactly what each number is,
where it comes from and what it is not.

## 1. Chronology

| Value | Meaning |
| --- | --- |
| `fetchedAtMillis` | Wall-clock time at which the payload was received. Shown as "updated Xm ago". |
| `current` | The model's analysis/now value for the current 15-minute step. This is **not** a station observation. |
| `hourly` | Hourly forecast values for the selected model, local time. |
| `daily` | Daily aggregates from the same model run. |
| `minutely_15` | 15-minute forecast/nowcast precipitation used for "next hour". |
| Radar `OBSERVED` | RainViewer past radar frame (measured reflectivity). |
| Radar `NOWCAST` | RainViewer short-range extrapolation. Not a measurement. |

## 2. Probability versus amount

- `precipitation_probability` — percent chance of measurable precipitation in that interval.
- `precipitation` — forecast water equivalent in mm for that interval.
- `precipitation_sum` — daily total amount.
- `precipitation_probability_max` — the day's maximum chance, not its amount.

ATMOS never multiplies probability by amount to display an "expected" value, and never converts one into
the other.

## 3. Missing data

Every nullable field stays nullable from JSON to UI. A missing UV value renders as `—` with
"Unavailable". No field is interpolated from neighbouring hours, climatology or a mock table.

If the network fails and a cache exists for the same coordinates, ATMOS parses and displays that raw payload
with a `Cached` label and the last update age. If no cache exists, the screen says the service could not be
reached and offers a retry.

## 4. Uncertainty

The Forecast tab requests the ECMWF IFS 0.25° ensemble (51 members). For each hour ATMOS computes the 10th,
50th and 90th percentile temperature and draws the 10–90 band. If fewer than five members are available for
an hour, that hour is omitted from the band. The band is never fabricated by adding a fixed ± value.

## 5. Lightning

The app never displays a lightning countdown, strike location or "next strike" estimate. The storm flash is
enabled only when the current WMO code is 95, 96 or 99, and it is explicitly described as an atmospheric
cue. Haptics are skipped when the ringer is silent/vibrate.

## 6. Severe weather

The hazard banner scans the next 24 hourly steps for thunderstorm codes, heavy-rain codes or gusts above
20 m/s. It is labelled **"Forecast signal, not an official warning"** and links to MeteoAlarm. ATMOS does
not parse CAP feeds or national warning APIs yet; it does not pretend otherwise.

## 7. Model selection

`Best match` lets Open-Meteo choose the strongest available model per coordinate. Explicit ECMWF, DWD ICON,
NOAA GFS and Météo-France AROME selections change the source, not the interpretation. The selected model is
shown on the home header, the forecast tab and inside the widget subtitle.

## 8. Provider failures

- Air quality failure → air card shows `— / Unavailable`; other values remain.
- Ensemble failure → uncertainty card/band hidden entirely.
- Radar failure → map still renders the basemap and states "Radar unavailable".
- Geocoding failure → search reports "Search unavailable"; saved cities still work.
