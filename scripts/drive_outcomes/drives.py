import json

DEF_TD = {'TURNOVER_TOUCHDOWN', 'KICK_SIX', 'RETURN_TOUCHDOWN', 'PUNT_RETURN_TOUCHDOWN'}
CONV_CALLS = {'PAT', 'TWO_POINT'}
KICKOFFS = {'KICKOFF_NORMAL', 'KICKOFF_ONSIDE', 'KICKOFF_SQUIB'}


def load(path):
    plays = json.load(open(path))
    return sorted(plays, key=lambda x: x['play_number'])


def segment(plays):
    drives = []
    cur = None
    for p in plays:
        if p['actual_result'] in ('END_OF_GAME', 'END_OF_HALF') or p['play_call'] is None:
            continue
        if p['play_call'] in KICKOFFS:
            cur = None
            continue
        if p['play_call'] in CONV_CALLS or (p['play_call'] in ('KNEEL', 'SPIKE') and p['actual_result'] == 'NO_GOOD'):
            if cur is not None:
                cur['conv'].append(p)
            continue
        new_period = cur is not None and p['quarter'] >= 5 and p['quarter'] != cur['plays'][-1]['quarter']
        if cur is None or cur['poss'] != p['possession'] or new_period or cur['ended']:
            cur = {'poss': p['possession'], 'plays': [], 'conv': [], 'ended': False}
            drives.append(cur)
        cur['plays'].append(p)
        if p['actual_result'] in {'TOUCHDOWN'} | DEF_TD:
            cur['ended'] = True
    for d in drives:
        d['outcome'] = outcome(d)
    return drives


def outcome(d):
    last = d['plays'][-1]
    if any(p['actual_result'] == 'TOUCHDOWN' for p in d['plays']):
        return 'TD'
    if last['actual_result'] in DEF_TD:
        return 'DEF_TD'
    if last['play_call'] == 'FIELD_GOAL' and last['actual_result'] == 'GOOD':
        return 'FG'
    return 'NONE'
