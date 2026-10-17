import copy
import gzip
import json

import pytest

from sls.audit.short_battle import load_corpus
from sls.rl.training_contract import native_source_digest, sha256_file


def write_corpus(directory, samples, baseline):
    (directory/'samples.json.gz').write_bytes(gzip.compress(json.dumps({'samples':samples}).encode()))
    (directory/'baseline.json').write_text(json.dumps(baseline))
    summary = {'native_source_sha256':native_source_digest(), 'roots':len(samples),
               'independent_sources_with_roots':len({s['source_seed'] for s in samples}),
               'files':{name:sha256_file(directory/name) for name in ('samples.json.gz','baseline.json')}}
    (directory/'summary.json').write_text(json.dumps(summary))


def rows():
    return [dict(sample_id='fit',source_seed=1500000000000,split='fit',
                 native_source_sha256=native_source_digest()),
            dict(sample_id='confirm',source_seed=8000102004000,split='confirmation',
                 native_source_sha256=native_source_digest())]


def test_valid_corpus_and_tampered_bytes(tmp_path):
    write_corpus(tmp_path,rows(),[{'sample_id':'fit'}])
    assert len(load_corpus(tmp_path)[1])==2
    (tmp_path/'baseline.json').write_text('[]')
    with pytest.raises(ValueError,match='identity mismatch'):
        load_corpus(tmp_path)


@pytest.mark.parametrize('fault,match',[
    ('duplicate','duplicate short-battle'),('leak','leaked across'),
    ('wrong_namespace','namespace mismatch'),('confirmation','confirmation outcomes'),
    ('missing_baseline','missing or duplicate'),('duplicate_baseline','missing or duplicate')])
def test_reject_semantic_data_faults_even_with_matching_hashes(tmp_path,fault,match):
    samples=copy.deepcopy(rows())
    baseline=[{'sample_id':'fit'}]
    if fault=='duplicate':
        samples.append(copy.deepcopy(samples[0]))
    elif fault=='leak':
        samples[1]['source_seed']=samples[0]['source_seed']
    elif fault=='wrong_namespace':
        samples[0]['source_seed']=8000102004001
    elif fault=='confirmation':
        baseline.append({'sample_id':'confirm'})
    elif fault=='missing_baseline':
        baseline=[]
    else:
        baseline.append({'sample_id':'fit'})
    write_corpus(tmp_path,samples,baseline)
    with pytest.raises(ValueError,match=match):
        load_corpus(tmp_path)
