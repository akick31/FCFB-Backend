import collections
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from drives import load, segment

plays_dir, out_path = sys.argv[1], sys.argv[2]

YTG_BOUNDS = [3, 6, 10, 15, 20, 25, 30, 35, 40, 50, 99]
DIST_BOUNDS = [2, 5, 10, 99]
MIN_SAMPLES = 40
LATE_HALF_SECONDS = 120
GARBAGE_MARGIN = 17


def bucket(value, bounds):
    return next(i for i, b in enumerate(bounds) if value <= b)


def clock_affected(drive):
    return any(p['quarter'] in (2, 4) and p['clock'] < LATE_HALF_SECONDS for p in drive['plays'])


def garbage_time(drive):
    first = drive['plays'][0]
    return first['quarter'] >= 3 and abs(first['home_score'] - first['away_score']) >= GARBAGE_MARGIN


def usable(drive):
    if drive['plays'][0]['quarter'] >= 5:
        return True
    return not clock_affected(drive) and not garbage_time(drive)


fine = collections.defaultdict(collections.Counter)
mid = collections.defaultdict(collections.Counter)
coarse = collections.defaultdict(collections.Counter)
conv = collections.Counter()
drive_count = 0

for path in glob.glob(plays_dir + '/*.json'):
    try:
        plays = load(path)
    except Exception:
        continue
    for p in plays:
        if p['play_call'] == 'PAT':
            conv[('PAT', p['actual_result'] == 'GOOD')] += 1
        if p['play_call'] == 'TWO_POINT':
            conv[('TWO_POINT', p['actual_result'] == 'SUCCESS')] += 1
    for d in segment(plays):
        if not usable(d):
            continue
        drive_count += 1
        for p in d['plays']:
            if p['play_call'] in ('KNEEL', 'SPIKE', 'PUNT') or p['down'] < 1:
                continue
            ytg = 100 - p['ball_location']
            if ytg < 1:
                continue
            dist = min(p['yards_to_go'], ytg)
            yb, db = bucket(ytg, YTG_BOUNDS), bucket(dist, DIST_BOUNDS)
            fine[(p['down'], db, yb)][d['outcome']] += 1
            mid[(p['down'], yb)][d['outcome']] += 1
            coarse[yb][d['outcome']] += 1


def probs(counter):
    n = sum(counter.values())
    return {
        'touchdown': round(counter['TD'] / n, 4),
        'fieldGoal': round(counter['FG'] / n, 4),
        'defensiveTouchdown': round(counter['DEF_TD'] / n, 4),
        'samples': n,
    }


cells = {}
sources = collections.Counter()
for down in range(1, 5):
    for db in range(len(DIST_BOUNDS)):
        for yb in range(len(YTG_BOUNDS)):
            for level, counter in (('fine', fine[(down, db, yb)]), ('mid', mid[(down, yb)]), ('coarse', coarse[yb])):
                if sum(counter.values()) >= MIN_SAMPLES:
                    cells[f'{down}:{db}:{yb}'] = probs(counter)
                    sources[level] += 1
                    break

def rate(kind):
    made, missed = conv[(kind, True)], conv[(kind, False)]
    return round(made / (made + missed), 4), made + missed

def second_possession_behavior(fresh_cell):
    needs_touchdown = collections.Counter()
    field_goal_helps = collections.Counter()
    for path in glob.glob(plays_dir + '/*.json'):
        try:
            plays = load(path)
        except Exception:
            continue
        periods = collections.defaultdict(list)
        for d in segment(plays):
            if d['plays'][0]['quarter'] >= 5:
                periods[d['plays'][0]['quarter']].append(d)
        for first, *rest in periods.values():
            if not rest or first['outcome'] == 'DEF_TD':
                continue
            target = needs_touchdown if first['outcome'] == 'TD' else field_goal_helps
            target[rest[0]['outcome']] += 1
    base_none = 1 - fresh_cell['touchdown'] - fresh_cell['fieldGoal'] - fresh_cell['defensiveTouchdown']
    td_rate = needs_touchdown['TD'] / sum(needs_touchdown.values())
    none_rate = field_goal_helps['NONE'] / sum(field_goal_helps.values())
    conversion = (td_rate - fresh_cell['touchdown']) / fresh_cell['fieldGoal']
    reduction = (base_none - none_rate) / base_none
    print('needs TD', sum(needs_touchdown.values()), 'td rate', round(td_rate, 3), 'conversion', round(conversion, 4))
    print('FG helps', sum(field_goal_helps.values()), 'empty rate', round(none_rate, 3), 'reduction', round(reduction, 4))
    return round(conversion, 4), round(reduction, 4)


fresh_key = f'1:{bucket(10, DIST_BOUNDS)}:{bucket(25, YTG_BOUNDS)}'
field_goal_conversion, no_score_reduction = second_possession_behavior(cells[fresh_key])

pat, pat_n = rate('PAT')
two, two_n = rate('TWO_POINT')
json.dump({
    'yardsToGoalBounds': YTG_BOUNDS,
    'distanceBounds': DIST_BOUNDS,
    'patSuccessRate': pat,
    'twoPointSuccessRate': two,
    'touchdownNeededFieldGoalConversion': field_goal_conversion,
    'fieldGoalSufficientNoScoreReduction': no_score_reduction,
    'cells': {k: {kk: vv for kk, vv in v.items() if kk != 'samples'} for k, v in cells.items()},
}, open(out_path, 'w'), indent=2)
print('drives', drive_count, 'cells', len(cells), dict(sources))
print('PAT', pat, pat_n, '2PT', two, two_n)
