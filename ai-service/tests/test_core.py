import unittest

from app.core.cache import TTLCache
from app.core.config import ConfigError, Settings
from app.core.errors import error_body
from app.core.security import is_valid_api_key
from app.core.serialization import camel, to_api
from app.search.models import Paper, ProviderStatus

KEY = "a" * 40


class FakeClock:
    def __init__(self):
        self.now = 0.0

    def __call__(self):
        return self.now


class CacheTests(unittest.TestCase):
    def test_entries_expire_after_the_ttl(self):
        clock = FakeClock()
        cache = TTLCache(10, 5, clock)
        cache.set("k", "v")
        clock.now = 9.9
        self.assertEqual(cache.get("k"), "v")
        clock.now = 10.0
        self.assertIsNone(cache.get("k"))
        self.assertEqual(len(cache), 0)

    def test_least_recently_used_entry_is_dropped_when_full(self):
        cache = TTLCache(100, 2, FakeClock())
        cache.set("a", 1)
        cache.set("b", 2)
        cache.get("a")          # a is now the most recently used
        cache.set("c", 3)
        self.assertIsNone(cache.get("b"))
        self.assertEqual(cache.get("a"), 1)
        self.assertEqual(cache.get("c"), 3)

    def test_invalid_limits_are_rejected(self):
        with self.assertRaises(ValueError):
            TTLCache(0, 5)
        with self.assertRaises(ValueError):
            TTLCache(5, 0)


class ConfigTests(unittest.TestCase):
    def test_a_short_or_missing_key_stops_startup(self):
        for env in ({}, {"AI_SERVICE_API_KEY": ""}, {"AI_SERVICE_API_KEY": "short"}):
            with self.assertRaises(ConfigError):
                Settings.from_env(env)

    def test_defaults(self):
        settings = Settings.from_env({"AI_SERVICE_API_KEY": KEY})
        self.assertEqual(settings.api_key, KEY)
        self.assertEqual((settings.request_timeout_seconds, settings.cache_ttl_seconds, settings.cache_max_entries),
                         (10.0, 3600, 500))
        self.assertFalse(settings.enable_docs)
        self.assertEqual(settings.contact_email, "")

    def test_overrides(self):
        settings = Settings.from_env({
            "AI_SERVICE_API_KEY": f" {KEY} ", "CONTACT_EMAIL": "team@example.org", "PROVIDER_TIMEOUT_SECONDS": "4.5",
            "CACHE_TTL_SECONDS": "60", "AI_SERVICE_ENABLE_DOCS": "true", "AI_SERVICE_LOG_LEVEL": "debug",
        })
        self.assertEqual(settings.api_key, KEY)
        self.assertEqual((settings.contact_email, settings.request_timeout_seconds, settings.cache_ttl_seconds),
                         ("team@example.org", 4.5, 60))
        self.assertTrue(settings.enable_docs)
        self.assertEqual(settings.log_level, "DEBUG")

    def test_bad_numbers_are_rejected(self):
        for bad in ("abc", "0", "-3"):
            with self.assertRaises(ConfigError):
                Settings.from_env({"AI_SERVICE_API_KEY": KEY, "CACHE_TTL_SECONDS": bad})


class SecurityTests(unittest.TestCase):
    def test_only_the_exact_key_is_accepted(self):
        self.assertTrue(is_valid_api_key(KEY, KEY))
        for provided in (None, "", KEY + "x", KEY[:-1], "b" * 40):
            self.assertFalse(is_valid_api_key(provided, KEY))
        self.assertFalse(is_valid_api_key(KEY, ""))


class SerializationTests(unittest.TestCase):
    def test_camel_case_and_enums_and_tuples(self):
        paper = Paper(title="T", sources=("openalex",), citation_count=5)
        data = to_api({"paper": paper, "status": ProviderStatus.NO_RESULTS})
        self.assertEqual(data["status"], "no_results")
        self.assertEqual(data["paper"]["citationCount"], 5)
        self.assertEqual(data["paper"]["sources"], ["openalex"])
        self.assertEqual(camel("papers_found"), "papersFound")

    def test_error_body_has_the_backend_shape(self):
        body = error_body("VALIDATION_ERROR", "bad", [{"field": "title", "message": "too short"}])
        self.assertEqual(set(body), {"success", "code", "message", "timestamp", "errors"})
        self.assertFalse(body["success"])
        self.assertTrue(body["timestamp"].endswith("Z"))
        self.assertNotIn("errors", error_body("X", "y"))


if __name__ == "__main__":
    unittest.main()
