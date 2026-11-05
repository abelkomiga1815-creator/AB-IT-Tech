"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/Alert";
import { Loading } from "@/components/Loading";
import { api, messageOf } from "@/lib/api";
import { DEFENSE_CATEGORIES, labelOf } from "@/lib/constants";
import type { DefenseCategory, DefenseQuestion } from "@/lib/types";
import { useLoad } from "@/lib/useLoad";

export function DefensePanel({ projectId }: { projectId: string }) {
  const base = `student/projects/${projectId}/defense-questions`;
  const questions = useLoad(() => api.get<DefenseQuestion[]>(base));
  const [question, setQuestion] = useState("");
  const [category, setCategory] = useState<DefenseCategory>("GENERAL");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(base, { question, category, prepared: false });
      setQuestion("");
      questions.reload();
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  const prepared = questions.data?.filter((item) => item.prepared).length ?? 0;
  const total = questions.data?.length ?? 0;

  return (
    <section className="card section-block">
      <h2>Defense preparation</h2>
      <p className="muted">Write the questions you expect and prepare your answers. {total > 0 ? `${prepared} of ${total} prepared.` : ""}</p>
      {error && <Alert>{error}</Alert>}
      <form className="form" onSubmit={onSubmit}>
        <div className="form-row">
          <label htmlFor="q-question">New question</label>
          <input id="q-question" value={question} maxLength={1000} required onChange={(e) => setQuestion(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="q-category">Category</label>
          <select id="q-category" value={category} onChange={(e) => setCategory(e.target.value as DefenseCategory)}>
            {DEFENSE_CATEGORIES.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy}>Add question</button>
      </form>

      {questions.loading && <Loading />}
      {questions.error && <Alert>{questions.error}</Alert>}
      {questions.data && questions.data.length === 0 && <p className="muted">No questions yet.</p>}
      {questions.data && questions.data.length > 0 && (
        <ul className="question-list">
          {questions.data.map((item) => (
            <QuestionItem key={item.id} base={base} item={item} onChanged={questions.reload} />
          ))}
        </ul>
      )}
    </section>
  );
}

function QuestionItem({ base, item, onChanged }: { base: string; item: DefenseQuestion; onChanged: () => void }) {
  const [answer, setAnswer] = useState(item.answer ?? "");
  const [prepared, setPrepared] = useState(item.prepared);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    setError(null);
    try {
      await api.put(`${base}/${item.id}`, {
        question: item.question,
        answer: answer || null,
        category: item.category,
        prepared,
      });
      onChanged();
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm("Delete this question?")) {
      return;
    }
    try {
      await api.delete(`${base}/${item.id}`);
      onChanged();
    } catch (failure) {
      setError(messageOf(failure));
    }
  }

  return (
    <li className="question-item">
      <div className="row-between">
        <strong>{item.question}</strong>
        <span className="badge">{labelOf(DEFENSE_CATEGORIES, item.category)}</span>
      </div>
      {error && <Alert>{error}</Alert>}
      <label className="sr-only" htmlFor={`a-${item.id}`}>Answer</label>
      <textarea id={`a-${item.id}`} rows={3} maxLength={4000} placeholder="Your prepared answer..." value={answer} onChange={(e) => setAnswer(e.target.value)} />
      <div className="row-between">
        <label className="check">
          <input type="checkbox" checked={prepared} onChange={(e) => setPrepared(e.target.checked)} /> Prepared
        </label>
        <div className="doc-actions">
          <button type="button" className="btn btn-primary btn-small" disabled={busy} onClick={save}>Save</button>
          <button type="button" className="btn btn-danger btn-small" onClick={remove}>Delete</button>
        </div>
      </div>
    </li>
  );
}
